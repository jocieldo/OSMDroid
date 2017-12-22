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
package com.rgi.geopackage.extensions;

import com.rgi.common.util.jdbc.JdbcUtility;
import com.rgi.geopackage.utility.DatabaseUtility;
import com.rgi.geopackage.verification.Assert;
import com.rgi.geopackage.verification.AssertionError;
import com.rgi.geopackage.verification.ColumnDefinition;
import com.rgi.geopackage.verification.Requirement;
import com.rgi.geopackage.verification.Severity;
import com.rgi.geopackage.verification.TableDefinition;
import com.rgi.geopackage.verification.UniqueDefinition;
import com.rgi.geopackage.verification.VerificationLevel;
import com.rgi.geopackage.verification.Verifier;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jenifer Cochran
 * @author Luke Lambert
 */
public class ExtensionsVerifier extends Verifier {
    private static final class ExtensionData {
        ExtensionData(final String tableName,
                      final String columnName,
                      final String extensionName) {
            this.tableName = tableName;
            this.columnName = columnName;
            this.extensionName = extensionName;
        }

        public String getTableName() {
            return this.tableName;
        }

        public String getColumnName() {
            return this.columnName;
        }

        public String getExtensionName() {
            return this.extensionName;
        }

        private final String tableName;
        private final String columnName;
        private final String extensionName;
    }

    private final boolean hasGpkgExtensionsTable;

    private List<ExtensionData> gpkgExtensionsDataAndColumnName;

    /**
     * Constructor
     *
     * @param verificationLevel Controls the level of verification testing performed
     * @param sqliteConnection  A connection handle to the database
     * @throws SQLException if test initialization fails to get information from the
     *                      database
     */
    public ExtensionsVerifier(final Connection sqliteConnection, final VerificationLevel verificationLevel) throws SQLException {
        super(sqliteConnection, verificationLevel);

        this.hasGpkgExtensionsTable = DatabaseUtility.doesTableOrViewExists(this.getSqliteConnection(), GeoPackageExtensions.ExtensionsTableName);

        if (this.hasGpkgExtensionsTable) {
            final String query = String.format("SELECT table_name, column_name, extension_name FROM %s;", GeoPackageExtensions.ExtensionsTableName);

            this.gpkgExtensionsDataAndColumnName = JdbcUtility.select(this.getSqliteConnection(),
                    query,
                    null,
                    resultSet -> new ExtensionData(resultSet.getString(1),
                            resultSet.getString(2),
                            resultSet.getString(3)));
        }
    }

    /**
     * Requirement 79
     * <p>
     * <blockquote> A GeoPackage MAY contain a table or update table view named
     * gpkg_extensions. If present this table SHALL be defined per clause
     * 2.5.2.1.1 <a
     * href="http://www.geopackage.org/spec/#extensions_table_definition">Table
     * Definition</a>, <a
     * href="http://www.geopackage.org/spec/#gpkg_extensions_cols">GeoPackage
     * Extensions Table or View Definition (Table or View Name:
     * gpkg_extensions)</a> and <a
     * href="http://www.geopackage.org/spec/#gpkg_extensions_sql">
     * gpkg_extensions Table Definition SQL</a>.
     * </blockquote>
     *
     * @throws SQLException   throws when various SQLExceptions occur
     * @throws AssertionError throws when the GeoPackage Fails to meet this requirement
     */
    @Requirement(reference = "Requirement 79",
            text = "A GeoPackage MAY contain a table or updateable view named gpkg_extensions. If present this table SHALL be defined per clause 2.5.2.1.1 Table Definition, GeoPackage Extensions Table or View Definition (Table or View Name: gpkg_extensions) and gpkg_extensions Table Definition SQL.")
    public void requirement79() throws AssertionError, SQLException {
        if (this.hasGpkgExtensionsTable) {
            this.verifyTable(ExtensionsVerifier.ExtensionsTableDefinition);
        }
    }


    /**
     * Requirement 80
     * <p>
     * <blockquote>
     * Every extension of a GeoPackage SHALL be registered in a corresponding
     * row in the gpkg_extensions table. The absence of a gpkg_extensions table
     * or the absence of rows in gpkg_extensions table SHALL both indicate the
     * absence of extensions to a GeoPackage.
     * </blockquote>
     */
    @Requirement(reference = "Requirement 80",
            text = "Every extension of a GeoPackage SHALL be registered in a corresponding row in the gpkg_extensions table. The absence of a gpkg_extensions table or the absence of rows in gpkg_extensions table SHALL both indicate the absence of extensions to a GeoPackage.")
    public void requirement80() {
        // TODO implement this requirement
        // Check if it has geometry_columns table
        // if it does check geometry_type_name,
        // if in Annex E
        // if it is not in the extensions table under extension_name = gpkg_geo_<geometry_type_name>, throw assertion Error
        // else not in annex e
        // extension name does not begin with gpkg and extension name ends with geom<geometry_type_name>
        // check master table for rtree% table
        // check if extension name has gpkg_rtree_index fail if doesn't
        // check master table for fgti_%
        // fail if extension_name != gpkg_srs_id_trigger

        // use Severity.Warning
    }

    /**
     * Requirement 81
     * <p>
     * <blockquote> Values of the {@code gpkg_extensions} {@code table_name
     * } column SHALL reference values in the {@code gpkg_contents}
     * {@code table_name} column or be NULL. They SHALL NOT be NULL for
     * rows where the {@code column_name} value is not NULL.
     * </blockquote>
     *
     * @throws SQLException   throws when various SQLExceptions occur
     * @throws AssertionError throws when the GeoPackage Fails to meet this requirement
     */
    @Requirement(reference = "Requirement 81",
            text = "Values of the gpkg_extensions table_name column SHALL reference values in the gpkg_contents table_name column or be NULL. They SHALL NOT be NULL for rows where the column_name value is not NULL.")
    public void requirement81() throws SQLException, AssertionError {
        if (this.hasGpkgExtensionsTable) {
            for (final ExtensionData extensionData : this.gpkgExtensionsDataAndColumnName) {
                final String columnName = extensionData.getColumnName();

                final boolean validEntry = extensionData.getTableName() != null || columnName == null; // If table name is null then so must column name

                Assert.assertTrue("The value in table_name can only be null if column_name is also null.",
                        validEntry,
                        Severity.Warning);
            }

            // Check that the table_name in GeoPackage Extensions references a table in sqlite master
            final String query = String.format("SELECT table_name " +
                            "FROM   %s " +
                            "WHERE  table_name COLLATE NOCASE NOT IN " +
                            "   (SELECT tbl_name " +
                            "    FROM   sqlite_master " +
                            "    WHERE  (type = 'table' OR type = 'view'));",
                    GeoPackageExtensions.ExtensionsTableName);

            final List<String> nonExistantExtensionsTable = JdbcUtility.select(this.getSqliteConnection(),
                    query,
                    null,
                    result -> result.getString(1));

            Assert.assertTrue(String.format("The following table(s) does not exist in the sqlite master table. Either create table following table(s) or delete this entry in %s.\n %s",
                    GeoPackageExtensions.ExtensionsTableName,
                    nonExistantExtensionsTable.stream()
                            .map(table -> String.format("\t%s", table))
                            .collect(Collectors.joining("\n"))),
                    nonExistantExtensionsTable.isEmpty(),
                    Severity.Warning);
        }
    }

    /**
     * Requirement 82 <blockquote> The
     * {@code column_name} column value in a {@code gpkg_extensions}
     * row SHALL be the name of a column in the table specified by the
     * {@code table_name} column value for that row, or be NULL.
     * </blockquote>
     *
     * @throws SQLException   throws when various SQLExceptions occur
     * @throws AssertionError throws when the GeoPackage Fails to meet this requirement
     */
    @Requirement(reference = "Requirement 82",
            text = "The column_name column value in a gpkg_extensions row SHALL be the name of a column in the table specified by the table_name column value for that row, or be NULL.")
    public void requirement82() throws SQLException, AssertionError {
        if (this.hasGpkgExtensionsTable && !this.gpkgExtensionsDataAndColumnName.isEmpty()) {
            for (final ExtensionData extensionData : this.gpkgExtensionsDataAndColumnName) {
                final String tableName = extensionData.getTableName();
                final String columnName = extensionData.getColumnName();

                if (tableName != null && columnName != null) {
                    final String query = String.format("PRAGMA table_info(%s);", tableName);

                    final boolean columnExists = JdbcUtility.select(this.getSqliteConnection(),
                            query,
                            null,
                            resultSet -> resultSet.getString("name"))
                            .stream()
                            .anyMatch(name -> name.equalsIgnoreCase(columnName));

                    Assert.assertTrue(String.format("The column %s does not exist in the table %s. Please either add this column to this table or delete the record in %s.",
                            columnName,
                            tableName,
                            GeoPackageExtensions.ExtensionsTableName),
                            columnExists,
                            Severity.Warning);
                }
            }
        }
    }

    /**
     * Requirement 83 <blockquote> Each
     * {@code extension_name} column value in a
     * {@code gpkg_extensions} row SHALL be a unique case sensitive value
     * of the form &lt;author&gt;_&lt;extension_name&gt; where &lt;author&gt;
     * indicates the person or organization that developed and maintains the
     * extension. The valid character set for <author> SHALL be [a-zA-Z0-9]. The
     * valid character set for &lt;extension_name&gt; SHALL be [a-zA-Z0-9_]. An
     * {@code extension_name} for the "gpkg" author name SHALL be one of
     * those defined in this encoding standard or in an OGC Best Practices
     * Document that extends it. </blockquote>
     *
     * @throws AssertionError throws when the GeoPackage Fails to meet this requirement
     */
    @Requirement(reference = "Requirement 83",
            text = "Each extension_name column value in a gpkg_extensions row SHALL be a unique case sensitive value of the form <author>_<extension_name> where <author> indicates the person or organization that developed and maintains the extension. The valid character set for <author> SHALL be [a-zA-Z0-9]. The valid character set for <extension_name> SHALL be [a-zA-Z0-9_]. An extension_name for the gpkg author name SHALL be one of those defined in this encoding standard or in an OGC Best Practices Document that extends it.")
    public void requirement83() throws AssertionError {
        if (this.hasGpkgExtensionsTable) {
            final Set<String> invalidExtensionNames = this.gpkgExtensionsDataAndColumnName.stream()
                    .map(ExtensionData::getExtensionName)
                    .filter(name -> {
                        if (name == null) {
                            return true;
                        }

                        final String[] author = name.split("_", 2);

                        return author.length != 2 ||
                                (author[0].matches("gpkg") && !isRegisteredExtension(name)) ||
                                !author[0].matches("[a-zA-Z0-9]+") ||
                                !author[1].matches("[a-zA-Z0-9_]+");
                    })
                    .collect(Collectors.toSet());

            Assert.assertTrue(String.format("The following extension_name(s) are invalid: \n%s",
                    invalidExtensionNames.stream()
                            .map(extensionName -> {
                                if (extensionName.isEmpty()) {
                                    return "\t<empty string>";
                                }

                                return String.format("\t%s", extensionName);
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", "))),
                    invalidExtensionNames.isEmpty(),
                    Severity.Warning);
        }
    }

    /**
     * Requirement 84 <blockquote> The definition
     * column value in a {@code gpkg_extensions} row SHALL contain or
     * reference the text that results from documenting an extension by filling
     * out the GeoPackage Extension Template in <a
     * href="http://www.geopackage.org/spec/#extension_template"> GeoPackage
     * Extension Template (Normative)</a>. </blockquote>
     *
     * @throws SQLException   throws when various SQLExceptions occur
     * @throws AssertionError throws when the GeoPackage Fails to meet this requirement
     */
    @Requirement(reference = "Requirement 84",
            text = "The definition column value in a gpkg_extensions row SHALL "
                    + "contain or reference the text that results from documenting "
                    + "an extension by filling out the GeoPackage Extension Template "
                    + "in GeoPackage Extension Template (Normative).")
    public void requirement84() throws SQLException, AssertionError {
        if (this.hasGpkgExtensionsTable) {
            final String query = String.format("SELECT table_name "
                            + "FROM %s "
                            + "WHERE definition NOT LIKE '%s' "
                            + "AND   definition NOT LIKE '%s' "
                            + "AND   definition NOT LIKE '%s' "
                            + "AND   definition NOT LIKE '%s';",
                    GeoPackageExtensions.ExtensionsTableName,
                    "Annex%",
                    "http%",
                    "mailto%",
                    "Extension Title%");

            final List<String> invalidDefinitions = JdbcUtility.select(this.getSqliteConnection(),
                    query,
                    null,
                    resultSet -> resultSet.getString(1));

            Assert.assertTrue(String.format("The following table_name values in %s table have invalid values for the definition column: %s.",
                    GeoPackageExtensions.ExtensionsTableName,
                    invalidDefinitions.stream()
                            .collect(Collectors.joining(", "))),
                    invalidDefinitions.isEmpty(),
                    Severity.Warning);
        }
    }

    /**
     * Requirement 85
     * <p>
     * <blockquote>
     * The scope column value in a {@code gpkg_extensions} row SHALL be
     * lowercase "read-write" for an extension that affects both readers and
     * writers, or "write-only" for an extension that affects only writers.
     * </blockquote>
     *
     * @throws SQLException   throws when various SQLExceptions occur
     * @throws AssertionError throws when the GeoPackage Fails to meet this requirement
     */
    @Requirement(reference = "Requirement 85",
            text = "The scope column value in a gpkg_extensions row SHALL be lowercase \"read-write\" for an extension that affects both readers and writers, or \"write-only\" for an extension that affects only writers.")
    public void requirement85() throws SQLException, AssertionError {
        if (this.hasGpkgExtensionsTable) {
            final String query = String.format("SELECT scope FROM %s WHERE scope != 'read-write' AND scope != 'write-only'",
                    GeoPackageExtensions.ExtensionsTableName);

            final List<String> invalidScope = JdbcUtility.select(this.getSqliteConnection(),
                    query,
                    null,
                    resultSet -> resultSet.getString(1));

            Assert.assertTrue(String.format("There is(are) value(s) in the column scope in %s table that is not 'read-write' or 'write-only' in all lowercase letters. The following values are incorrect: %s",
                    GeoPackageExtensions.ExtensionsTableName,
                    invalidScope.stream()
                            .collect(Collectors.joining(", "))),
                    invalidScope.isEmpty(),
                    Severity.Warning);
        }
    }

    private static boolean isRegisteredExtension(final String extensionName) {
        return RegisteredExtensions.contains(extensionName);
    }

    private static final TableDefinition ExtensionsTableDefinition;
    private static final List<String> RegisteredExtensions;

    static {
        final Map<String, ColumnDefinition> extensionsTableColumns = new HashMap<>();

        extensionsTableColumns.put("table_name", new ColumnDefinition("TEXT", false, false, false, null));
        extensionsTableColumns.put("column_name", new ColumnDefinition("TEXT", false, false, false, null));
        extensionsTableColumns.put("extension_name", new ColumnDefinition("TEXT", true, false, false, null));
        extensionsTableColumns.put("definition", new ColumnDefinition("TEXT", true, false, false, null));
        extensionsTableColumns.put("scope", new ColumnDefinition("TEXT", true, false, false, null));

        ExtensionsTableDefinition = new TableDefinition(GeoPackageExtensions.ExtensionsTableName,
                extensionsTableColumns,
                Collections.emptySet(),
                new HashSet<>(Arrays.asList(new UniqueDefinition("table_name", "column_name", "extension_name"))));

        RegisteredExtensions = Arrays.asList("gpkg_zoom_other", "gpkg_webp", "gpkg_geometry_columns", "gpkg_rtree_index", "gpkg_geometry_type_trigger", "gpkg_srs_id_trigger");
    }
}
