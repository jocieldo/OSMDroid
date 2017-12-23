/* The MIT License (MIT)
 *
 * Copyright (c) 2015 Reinventing Geospatial, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package rgi.geopackage.metadata;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import other.MimeType;
import rgi.common.util.jdbc.JdbcUtility;
import rgi.geopackage.utility.DatabaseUtility;
import rgi.geopackage.utility.SelectBuilder;
import rgi.geopackage.verification.VerificationIssue;
import rgi.geopackage.verification.VerificationLevel;

/**
 * @author Luke Lambert
 */
public class GeoPackageMetadata {
    /**
     * Constructor
     *
     * @param databaseConnection The open connection to the database that contains a GeoPackage
     */
    public GeoPackageMetadata(final Connection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Metadata requirements this GeoPackage failed to meet
     *
     * @param verificationLevel Controls the level of verification testing performed
     * @return The metadata GeoPackage requirements this GeoPackage fails to conform to
     * @throws SQLException throws when the {@link MetadataVerifier#MetadataVerifier} throws an SQLException
     */
    public Collection<VerificationIssue> getVerificationIssues(final VerificationLevel verificationLevel) throws SQLException {
        return new MetadataVerifier(this.databaseConnection, verificationLevel).getVerificationIssues();
    }

    /**
     * Creates an entry in the GeoPackage metadata table
     *
     * @param scope       Metadata scope
     * @param standardUri URI reference to the metadata structure definition authority
     * @param mimeType    MIME encoding of metadata
     * @param metadata    Metadata text
     * @return Returns the newly added {@link Metadata} object
     * @throws SQLException if a database access error occurs, this method is called
     *                      while participating in a distributed transaction, if this
     *                      method is called on a closed connection or this Connection
     *                      object is in auto-commit mode, or if the method getMetadata()
     *                      throws or other various SQLExceptions occur
     */
    public Metadata addMetadata(final Scope scope,
                                final URI standardUri,
                                final MimeType mimeType,
                                final String metadata) throws SQLException {
        if (scope == null) {
            throw new IllegalArgumentException("Scope may not be null");
        }

        if (standardUri == null) {
            throw new IllegalArgumentException("Standard URI may not be null");
        }

        if (mimeType == null) {
            throw new IllegalArgumentException("Mime type may not be null");
        }

        if (metadata == null) {
            throw new IllegalArgumentException("Metadata may not be null");
        }

        final Metadata existingMetadata = this.getMetadata(scope.toString(),
                standardUri.toString(),
                mimeType.toString(),
                metadata);

        if (existingMetadata != null) {
            return existingMetadata;
        }

        try {
            this.createMetadataTableNoCommit();  // Create the metadata table if it doesn't exist

            final String insertMetadataSql = String.format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                    GeoPackageMetadata.MetadataTableName,
                    "md_scope",
                    "md_standard_uri",
                    "mime_type",
                    "metadata");

            try (PreparedStatement preparedStatement = this.databaseConnection.prepareStatement(insertMetadataSql)) {
                preparedStatement.setString(1, scope.toString());
                preparedStatement.setString(2, standardUri.toString());
                preparedStatement.setString(3, mimeType.toString());
                preparedStatement.setString(4, metadata);

                preparedStatement.executeUpdate();
            }

            this.databaseConnection.commit();

            return this.getMetadata(scope.toString(),
                    standardUri.toString(),
                    mimeType.toString(),
                    metadata);
        } catch (final Exception ex) {
            this.databaseConnection.rollback();
            throw ex;
        }
    }

    /**
     * Creates an entry in the GeoPackage metadata reference table
     *
     * @param referenceScope   Reference scope
     * @param tableName        Name of the table to which this metadata reference applies, or
     *                         NULL for referenceScope of 'geopackage'
     * @param columnName       Name of the column to which this metadata reference applies;
     *                         NULL for referenceScope of 'geopackage','table' or 'row', or
     *                         the name of a column in the tableName table for referenceScope
     *                         of 'column' or 'row/col'
     * @param rowIdentifier    NULL for referenceScope of 'geopackage', 'table' or 'column',
     *                         or the rowed of a row record in the table_name table for
     *                         referenceScope of 'row' or 'row/col'
     * @param fileIdentifier   gpkg_metadata table identifier column value for the metadata
     *                         to which this gpkg_metadata_reference applies
     * @param parentIdentifier gpkg_metadata table identifier column value for the
     *                         hierarchical parent gpkg_metadata for the gpkg_metadata to
     *                         which this gpkg_metadata_reference applies, or NULL if file
     *                         identifier forms the root of a metadata hierarchy
     * @return Returns the newly added {@link MetadataReference} object
     * @throws SQLException throws if various SQLExceptions occur
     */
    public MetadataReference addMetadataReference(final ReferenceScope referenceScope,
                                                  final String tableName,
                                                  final String columnName,
                                                  final Integer rowIdentifier,
                                                  final Metadata fileIdentifier,
                                                  final Metadata parentIdentifier) throws SQLException {
        if (referenceScope == null) {
            throw new IllegalArgumentException("Reference scope may not be null");
        }

        if (referenceScope == ReferenceScope.GeoPackage && tableName != null) {
            throw new IllegalArgumentException("Reference scopes of 'geopackage' must have null for the associated table name, and other reference scope values must have non-null table names");    // Requirement 72
        }

        if (!ReferenceScope.isColumnScope(referenceScope) && columnName != null) {
            throw new IllegalArgumentException("Reference scopes 'geopackage', 'table' or 'row' must have a null column name. Reference scope values of 'column' or 'row/col' must have a non-null column name"); // Requirement 73
        }

        if (ReferenceScope.isRowScope(referenceScope) && rowIdentifier == null) {
            throw new IllegalArgumentException(String.format("Reference scopes of 'geopackage', 'table' or 'column' must have a null row identifier.  Reference scopes of 'row' or 'row/col', must contain a reference to a row record in the '%s' table",
                    tableName)); // Requirement 74
        }

        if (tableName != null && tableName.isEmpty()) {
            throw new IllegalArgumentException("If table name is non-null, it may not be empty");
        }

        if (columnName != null && columnName.isEmpty()) {
            throw new IllegalArgumentException("If column name is non-null, it may not be empty");
        }

        if (fileIdentifier == null) {
            throw new IllegalArgumentException("File identifier may not be null");
        }

        // TODO test referential integrity for table, column and row parameters - instead of using a table name, an Content object can be used

        final Integer parentIdInteger = parentIdentifier == null ? null
                : parentIdentifier.getIdentifier();

        final MetadataReference existingMetadataReference = this.getMetadataReference(referenceScope.getText(),
                tableName,
                columnName,
                rowIdentifier,
                fileIdentifier.getIdentifier(),
                parentIdInteger);
        if (existingMetadataReference != null) {
            return existingMetadataReference;
        }

        try {
            this.createMetadataReferenceTableNoCommit();  // Create the metadata reference table if it doesn't exist

            final String insertMetadataSql = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?)",
                    GeoPackageMetadata.MetadataReferenceTableName,
                    "reference_scope",
                    "table_name",
                    "column_name",
                    "row_id_value",
                    "md_file_id",
                    "md_parent_id");

            try (PreparedStatement preparedStatement = this.databaseConnection.prepareStatement(insertMetadataSql)) {
                preparedStatement.setString(1, referenceScope.getText());
                preparedStatement.setString(2, tableName);
                preparedStatement.setString(3, columnName);
                preparedStatement.setObject(4, rowIdentifier);
                preparedStatement.setInt(5, fileIdentifier.getIdentifier());
                preparedStatement.setObject(6, parentIdInteger);

                preparedStatement.executeUpdate();
            }

            this.databaseConnection.commit();

            return this.getMetadataReference(referenceScope.getText(),
                    tableName,
                    columnName,
                    rowIdentifier,
                    fileIdentifier.getIdentifier(),
                    parentIdInteger);
        } catch (final Exception ex) {
            this.databaseConnection.rollback();
            throw ex;
        }
    }

    /**
     * Gets all entries in the GeoPackage metadata table
     *
     * @return Returns a collection of {@link Metadata} objects
     * @throws SQLException throws if the method
     *                      {@link DatabaseUtility#doesTableOrViewExists(Connection, String)}
     *                      or if other various SQLExceptions occur
     */
    public Collection<Metadata> getMetadata() throws SQLException {
        if (!DatabaseUtility.doesTableOrViewExists(this.databaseConnection, GeoPackageMetadata.MetadataTableName)) {
            return Collections.emptyList();
        }

        final String metadataQuerySql = String.format("SELECT %s, %s, %s, %s, %s FROM %s;",
                "id",
                "md_scope",
                "md_standard_uri",
                "mime_type",
                "metadata",
                GeoPackageMetadata.MetadataTableName);

        return JdbcUtility.select(this.databaseConnection,
                metadataQuerySql,
                null,
                resultSet -> new Metadata(resultSet.getInt(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getString(5)));
    }

    /**
     * Gets an entry in the reference table which matches the supplied primary
     * key
     *
     * @param identifier Metadata primary key
     * @return Returns an instance of {@link Metadata} representing an entry in
     * the GeoPackage metadata table, or null if no entry matches the
     * supplied criteria
     * @throws SQLException throws if the method
     *                      {@link DatabaseUtility#doesTableOrViewExists(Connection, String)}
     *                      or if other various SQLExceptions occur
     */
    public Metadata getMetadata(final int identifier) throws SQLException {
        if (!DatabaseUtility.doesTableOrViewExists(this.databaseConnection, GeoPackageMetadata.MetadataTableName)) {
            return null;
        }

        final String metadataQuerySql = String.format("SELECT %s, %s, %s, %s FROM %s WHERE %s = ? LIMIT 1;",
                "md_scope",
                "md_standard_uri",
                "mime_type",
                "metadata",
                GeoPackageMetadata.MetadataTableName,
                "id");


        try (PreparedStatement preparedStatement = this.databaseConnection.prepareStatement(metadataQuerySql)) {
            preparedStatement.setInt(1, identifier);

            try (ResultSet result = preparedStatement.executeQuery()) {
                if (result.isBeforeFirst()) {
                    return new Metadata(identifier,
                            result.getString(1),  // scope
                            result.getString(2),  // URI
                            result.getString(3),  // mime type
                            result.getString(4)); // metadata
                }
            }

            return null;
        }
    }

    /**
     * Gets all entries in the GeoPackage metadata reference table
     *
     * @return Returns a collection of {@link MetadataReference} objects
     * @throws SQLException throws if the method
     *                      {@link DatabaseUtility#doesTableOrViewExists(Connection, String)}
     *                      or if other various SQLExceptions occur
     */
    public Collection<MetadataReference> getMetadataReferences() throws SQLException {
        if (!DatabaseUtility.doesTableOrViewExists(this.databaseConnection, GeoPackageMetadata.MetadataReferenceTableName)) {
            return Collections.emptyList();
        }

        final String metadataReferenceQuerySql = String.format("SELECT %s, %s, %s, %s, %s, %s, %s FROM %s;",
                "reference_scope",
                "table_name",
                "column_name",
                "row_id_value",
                "timestamp",
                "md_file_id",
                "md_parent_id",
                GeoPackageMetadata.MetadataReferenceTableName);

        return JdbcUtility.select(this.databaseConnection,
                metadataReferenceQuerySql,
                null,
                resultSet -> new MetadataReference(resultSet.getString(1),   // reference Scope
                        resultSet.getString(2),   // table name
                        resultSet.getString(3),   // column name
                        (Integer) resultSet.getObject(4),   // row identifier
                        resultSet.getString(5),   // timestamp
                        resultSet.getInt(6),   // file identifier
                        (Integer) resultSet.getObject(7))); // parent identifier
    }

    /**
     * Creates the GeoPackage metadata table
     * <br>
     * <br>
     * <b>**WARNING**</b> this does not do a database commit. It is expected
     * that this transaction will always be paired with others that need to be
     * committed or rollback as a single transaction.
     */
    private void createMetadataTableNoCommit() throws SQLException {
        // Create the tile matrix set table or view
        if (!DatabaseUtility.doesTableOrViewExists(this.databaseConnection, GeoPackageMetadata.MetadataTableName)) {
            JdbcUtility.update(this.databaseConnection,
                    getMetadataTableCreationSql());
        }
    }

    /**
     * Creates the GeoPackage metadata reference table
     * <br>
     * <br>
     * <b>**WARNING**</b> this does not do a database commit. It is expected
     * that this transaction will always be paired with others that need to be
     * committed or rollback as a single transaction.
     */
    private void createMetadataReferenceTableNoCommit() throws SQLException {
        // Create the tile matrix table or view
        if (!DatabaseUtility.doesTableOrViewExists(this.databaseConnection, GeoPackageMetadata.MetadataReferenceTableName)) {
            JdbcUtility.update(this.databaseConnection,
                    getMetadataReferenceTableCreationSql());
        }
    }

    private static String getMetadataTableCreationSql() {
        // http://www.geopackage.org/spec/#gpkg_metadata_cols
        // http://www.geopackage.org/spec/#gpkg_metadata_sql
        return "CREATE TABLE " + GeoPackageMetadata.MetadataTableName + '\n' +
                "(id              INTEGER CONSTRAINT m_pk PRIMARY KEY ASC NOT NULL UNIQUE,             -- Metadata primary key\n" +
                " md_scope        TEXT                                    NOT NULL DEFAULT 'dataset',  -- Case sensitive name of the data scope to which this metadata applies; see Metadata Scopes\n" +
                " md_standard_uri TEXT                                    NOT NULL,                    -- URI reference to the metadata structure definition authority\n" +
                " mime_type       TEXT                                    NOT NULL DEFAULT 'text/xml', -- MIME encoding of metadata\n" +
                " metadata        TEXT                                    NOT NULL                     -- metadata\n" +
                ");";
    }

    private static String getMetadataReferenceTableCreationSql() {
        // http://www.geopackage.org/spec/#gpkg_metadata_reference_cols
        // http://www.geopackage.org/spec/#gpkg_metadata_reference_sql
        return "CREATE TABLE " + GeoPackageMetadata.MetadataReferenceTableName + '\n' +
                "(reference_scope TEXT     NOT NULL,                                                -- Lowercase metadata reference scope; one of 'geopackage', 'table','column', 'row', 'row/col'\n" +
                " table_name      TEXT,                                                             -- Name of the table to which this metadata reference applies, or NULL for reference_scope of 'geopackage'\n" +
                " column_name     TEXT,                                                             -- Name of the column to which this metadata reference applies; NULL for reference_scope of 'geopackage','table' or 'row', or the name of a column in the table_name table for reference_scope of 'column' or 'row/col'\n" +
                " row_id_value    INTEGER,                                                          -- NULL for reference_scope of 'geopackage', 'table' or 'column', or the rowed of a row record in the table_name table for reference_scope of 'row' or 'row/col'\n" +
                " timestamp       DATETIME NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')), -- timestamp value in ISO 8601 format as defined by the strftime function '%Y-%m-%dT%H:%M:%fZ' format string applied to the current time\n" +
                " md_file_id      INTEGER  NOT NULL,                                                -- gpkg_metadata table id column value for the metadata to which this gpkg_metadata_reference applies\n" +
                " md_parent_id    INTEGER,                                                          -- gpkg_metadata table id column value for the hierarchical parent gpkg_metadata for the gpkg_metadata to which this gpkg_metadata_reference applies, or NULL if md_file_id forms the root of a metadata hierarchy\n" +
                " CONSTRAINT crmr_mfi_fk FOREIGN KEY (md_file_id) REFERENCES gpkg_metadata(id),\n" +
                " CONSTRAINT crmr_mpi_fk FOREIGN KEY (md_parent_id) REFERENCES gpkg_metadata(id));";
    }

    /**
     * Gets an entry in the metadata table that matches the supplied criteria
     *
     * @param scope       Case sensitive name of the data scope to which this metadata applies
     * @param standardUri URI reference to the metadata structure definition authority
     * @param mimeType    MIME encoding of metadata
     * @param metadata    Metadata text
     * @return Returns an an instance of {@link Metadata} representing an entry in the GeoPackage metadata table, or null if no entry matches the supplied criteria
     */
    private Metadata getMetadata(final String scope,
                                 final String standardUri,
                                 final String mimeType,
                                 final String metadata) throws SQLException {
        if (!DatabaseUtility.doesTableOrViewExists(this.databaseConnection, GeoPackageMetadata.MetadataTableName)) {
            return null;
        }

        final String metadataQuerySql = String.format("SELECT %s FROM %s WHERE %s = ? AND %s = ? AND %s = ? AND %s = ? LIMIT 1;",
                "id",
                GeoPackageMetadata.MetadataTableName,
                "md_scope",
                "md_standard_uri",
                "mime_type",
                "metadata");


        try (PreparedStatement preparedStatement = this.databaseConnection.prepareStatement(metadataQuerySql)) {
            preparedStatement.setString(1, scope);
            preparedStatement.setString(2, standardUri);
            preparedStatement.setString(3, mimeType);
            preparedStatement.setString(4, metadata);

            try (ResultSet result = preparedStatement.executeQuery()) {
                if (result.isBeforeFirst()) {
                    return new Metadata(result.getInt(1), // identifier
                            scope,
                            standardUri,
                            mimeType,
                            metadata);
                }

                return null;
            }
        }
    }

    /**
     * Gets an entry in the metadata reference table that matches the supplied criteria
     *
     * @param referenceScope   Reference scope
     * @param tableName        Name of the table to which this metadata reference applies, or NULL for referenceScope of 'geopackage'
     * @param columnName       Name of the column to which this metadata reference applies; NULL for referenceScope of 'geopackage','table' or 'row', or the name of a column in the tableName table for referenceScope of 'column' or 'row/col'
     * @param rowIdentifier    NULL for referenceScope of 'geopackage', 'table' or 'column', or the rowed of a row record in the table_name table for referenceScope of 'row' or 'row/col'
     * @param fileIdentifier   gpkg_metadata table identifier column value for the metadata to which this gpkg_metadata_reference applies
     * @param parentIdentifier gpkg_metadata table identifier column value for the hierarchical parent gpkg_metadata for the gpkg_metadata to which this gpkg_metadata_reference applies, or NULL if file identifier forms the root of a metadata hierarchy
     * @return Returns an instance of {@link MetadataReference} representing an entry in the GeoPackage metadata reference table, or null if no entry matches the supplied criteria
     */
    private MetadataReference getMetadataReference(final String referenceScope,
                                                   final String tableName,
                                                   final String columnName,
                                                   final Integer rowIdentifier,
                                                   final int fileIdentifier,
                                                   final Integer parentIdentifier) throws SQLException {
        if (!DatabaseUtility.doesTableOrViewExists(this.databaseConnection, GeoPackageMetadata.MetadataReferenceTableName)) {
            return null;
        }

        try (final SelectBuilder selectStatement = new SelectBuilder(this.databaseConnection,
                GeoPackageMetadata.MetadataReferenceTableName,
                Arrays.asList("timestamp"),
                Arrays.asList(new AbstractMap.SimpleImmutableEntry<>("reference_scope", referenceScope),
                        new AbstractMap.SimpleImmutableEntry<>("table_name", tableName),
                        new AbstractMap.SimpleImmutableEntry<>("column_name", columnName),
                        new AbstractMap.SimpleImmutableEntry<>("row_id_value", rowIdentifier),
                        new AbstractMap.SimpleImmutableEntry<>("md_file_id", fileIdentifier),
                        new AbstractMap.SimpleImmutableEntry<>("md_parent_id", parentIdentifier)));
             final ResultSet result = selectStatement.executeQuery())

        {
            if (result.isBeforeFirst()) {
                return new MetadataReference(referenceScope,
                        tableName,
                        columnName,
                        rowIdentifier,
                        result.getString(1),   // timestamp
                        fileIdentifier,
                        parentIdentifier);
            }

            return null;
        }
    }

    private final Connection databaseConnection;

    /**
     * The String name "gpkg_metadata" of the database Metadata table containing
     * the metadata of the GeoPackage
     * (http://www.geopackage.org/spec/#_metadata_table)
     */
    public static final String MetadataTableName = "gpkg_metadata";
    /**
     * The String name "gpkg_metadata_reference" of the database Metadata
     * Reference table containing the metadata references of the GeoPackage
     * http://www.geopackage.org/spec/#_metadata_reference_table
     */
    public static final String MetadataReferenceTableName = "gpkg_metadata_reference";
}
