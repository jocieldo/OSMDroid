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

package com.rgi.geopackage.schema;

import com.rgi.common.util.jdbc.JdbcUtility;
import com.rgi.geopackage.core.GeoPackageCore;
import com.rgi.geopackage.utility.DatabaseUtility;
import com.rgi.geopackage.verification.Assert;
import com.rgi.geopackage.verification.AssertionError;
import com.rgi.geopackage.verification.ColumnDefinition;
import com.rgi.geopackage.verification.ForeignKeyDefinition;
import com.rgi.geopackage.verification.Requirement;
import com.rgi.geopackage.verification.Severity;
import com.rgi.geopackage.verification.TableDefinition;
import com.rgi.geopackage.verification.UniqueDefinition;
import com.rgi.geopackage.verification.VerificationLevel;
import com.rgi.geopackage.verification.Verifier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jenifer Cochran
 * @author Luke Lambert
 */
@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")   // TODO create accessors
public class SchemaVerifier extends Verifier {
    // TODO don't use these inner classes
    private static final class DataColumns {
        private String tableName;
        private String columnName;
        private String constraintName;
    }

    private static final class DataColumnConstraints {
        private String constraintName;
        private String constraintType;
        private String value;
        private Double min;
        private Boolean minIsInclusive;
        private Double max;
        private Boolean maxIsInclusive;

        public String invalidMinMaxWithRangeType() {
            return String.format("constraint_name: %10s, constraint_type: %5s, invalid min: %.3f, invalid max: %.3f.",
                    this.constraintName,
                    this.constraintType,
                    this.min,
                    this.max);
        }
    }

    private final boolean hasDataColumnsTable;
    private final boolean hasDataColumnsConstraintsTable;
    private final List<DataColumns> dataColumnsValues;
    private final List<DataColumnConstraints> dataColumnConstraintsValues;

    /**
     * @param sqliteConnection  A handle to the database connection
     * @param verificationLevel Controls the level of verification testing performed
     * @throws SQLException throws if the method {@link DatabaseUtility#doesTableOrViewExists(Connection, String)} throws
     */
    public SchemaVerifier(final Connection sqliteConnection, final VerificationLevel verificationLevel) throws SQLException {
        super(sqliteConnection, verificationLevel);

        this.hasDataColumnsTable = DatabaseUtility.doesTableOrViewExists(this.getSqliteConnection(), GeoPackageSchema.DataColumnsTableName);
        this.hasDataColumnsConstraintsTable = DatabaseUtility.doesTableOrViewExists(this.getSqliteConnection(), GeoPackageSchema.DataColumnConstraintsTableName);

        this.dataColumnsValues = this.hasDataColumnsTable ? this.getDataColumnValues() : Collections.emptyList();
        this.dataColumnConstraintsValues = this.hasDataColumnsConstraintsTable ? this.getDataColumnConstraintsValues() : Collections.emptyList();
    }

    /**
     * Requirement 57
     * <p>
     * <blockquote>
     * A GeoPackage MAY contain a table or updateable view named
     * {@code gpkg_data_columns}. If present it SHALL be defined per
     * clause 2.3.2.1.1 <a href=
     * "http://www.geopackage.org/spec/#schema_data_columns_table_definition"
     * >Table Definition</a>, <a
     * href="http://www.geopackage.org/spec/#gpkg_data_columns_cols">Data
     * Columns Table or View Definition</a> and <a
     * href="http://www.geopackage.org/spec/#gpkg_data_columns_sql"
     * >gpkg_data_columns Table Definition SQL</a>.
     * </blockquote>
     *
     * @throws SQLException   throws if the method verifyTable throws an SQLException
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 57",
            text = "A GeoPackage MAY contain a table or updateable view named "
                    + "gpkg_data_columns. If present it SHALL be defined per "
                    + "clause 2.3.2.1.1 Table Definition, Data Columns Table or"
                    + " View Definition and gpkg_data_columns Table Definition SQL. ")
    public void Requirement57() throws AssertionError, SQLException {
        if (this.hasDataColumnsTable) {
            this.verifyTable(SchemaVerifier.DataColumnsTableDefinition);
        }
    }

    /**
     * Requirement 58
     * <p>
     * <blockquote>
     * Values of the {@code gpkg_data_columns} table {@code
     * table_name} column value SHALL reference values in the {@code
     * gpkg_contents} {@code table_name} column.
     * </blockquote>
     *
     * @throws SQLException   throws if various SQLExceptions occur
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 58",
            text = "Values of the gpkg_data_columns table table_name column value "
                    + "SHALL reference values in the gpkg_contents table_name column.")
    public void Requirement58() throws SQLException, AssertionError {
        if (this.hasDataColumnsTable) {
            final String query = String.format("SELECT dc.table_name "
                            + "FROM %s AS dc "
                            + "WHERE dc.table_name COLLATE NOCASE NOT IN (SELECT gc.table_name "
                            + "FROM %s AS gc);",
                    GeoPackageSchema.DataColumnsTableName,
                    GeoPackageCore.ContentsTableName);

            final List<String> invalidTableNames = JdbcUtility.select(this.getSqliteConnection(),
                    query,
                    null,
                    resultSet -> resultSet.getString(1));

            Assert.assertTrue(String.format("The following table_name(s) is(are) from %s and is(are) not referenced in the %s table_name: %s",
                    GeoPackageSchema.DataColumnsTableName,
                    GeoPackageCore.ContentsTableName,
                    invalidTableNames.stream()
                            .collect(Collectors.joining(", "))),
                    invalidTableNames.isEmpty(),
                    Severity.Error);
        }
    }

    /**
     * Requirement 59
     * <p>
     * <blockquote>
     * The {@code column_name} column value in a {@code gpkg_data_columns
     * } table row SHALL contain the name of a column in the SQLite table
     * or view identified by the {@code table_name} column value.
     * </blockquote>
     *
     * @throws SQLException   throws if various SQLExceptions occur
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 59",
            text = "The column_name column value in a gpkg_data_columns table row "
                    + "SHALL contain the name of a column in the SQLite table or view "
                    + "identified by the table_name column value. ")
    public void Requirement59() throws SQLException, AssertionError {
        if (this.hasDataColumnsTable) {
            for (final DataColumns dataColumn : this.dataColumnsValues) {
                if (DatabaseUtility.doesTableOrViewExists(this.getSqliteConnection(), dataColumn.tableName)) {
                    final String query = String.format("PRAGMA table_info(%s);", dataColumn.tableName);

                    final boolean columnExists = JdbcUtility.select(this.getSqliteConnection(),
                            query,
                            null,
                            resultSet -> resultSet.getString("name"))
                            .stream()
                            .anyMatch(name -> name.equalsIgnoreCase(dataColumn.columnName));

                    Assert.assertTrue(String.format("The column %s does not exist in the table %s.",
                            dataColumn.columnName,
                            dataColumn.tableName),
                            columnExists,
                            Severity.Warning);
                }
            }
        }
    }

    /**
     * Requirement 60
     * <p>
     * <blockquote>
     * The constraint_name column value in a gpkg_data_columns table MAY be
     * NULL. If it is not NULL, it SHALL contain a case sensitive
     * constraint_name column value from the gpkg_data_column_constraints
     * table.
     * </blockquote>
     *
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 60",
            text = "The constraint_name column value in a gpkg_data_columns table MAY be NULL. "
                    + "If it is not NULL, it SHALL contain a case sensitive constraint_name "
                    + "column value from the gpkg_data_column_constraints table. ")
    public void Requirement60() throws AssertionError {
        if (this.hasDataColumnsTable && this.hasDataColumnsConstraintsTable) {
            for (final DataColumnConstraints dataColumnConstraints : this.dataColumnConstraintsValues) {
                if (dataColumnConstraints.constraintName != null) {
                    final boolean containsConstraint = this.dataColumnsValues.stream()
                            .filter(dataColumn -> dataColumn.constraintName != null)
                            .anyMatch(dataColumn -> dataColumn.constraintName.equals(dataColumnConstraints.constraintName));

                    Assert.assertTrue(String.format("The constraint_name %s in %s is not referenced in %s table in the column constraint_name.",
                            dataColumnConstraints.constraintName,
                            GeoPackageSchema.DataColumnsTableName,
                            GeoPackageSchema.DataColumnConstraintsTableName),
                            containsConstraint,
                            Severity.Warning);
                }
            }
        }
    }

    /**
     * Requirement 61
     * <p>
     * <blockquote>
     * A GeoPackage MAY contain a table or updateable view named
     * gpkg_data_column_constraints. If present it SHALL be defined per clause
     * 2.3.3.1.1 <a href=
     * "http://www.geopackage.org/spec/#data_column_constraints_table_definition"
     * >Table Definition</a>, <a href=
     * "http://www.geopackage.org/spec/#gpkg_data_column_constraints_cols">Data
     * Column Constraints Table or View Definition</a> and <a
     * href="http://www.geopackage.org/spec/#gpkg_data_column_constraints_sql"
     * >gpkg_data_columns Table Definition SQL</a>.
     * </blockquote>
     *
     * @throws SQLException   throws if the method verifyTable throws
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 61",
            text = "A GeoPackage MAY contain a table or updateable view named "
                    + "gpkg_data_column_constraints. If present it SHALL be defined "
                    + "per clause 2.3.3.1.1 Table Definition, Data Column Constraints "
                    + "Table or View Definition and gpkg_data_columns Table Definition SQL. ")
    public void Requirement61() throws AssertionError, SQLException {
        if (this.hasDataColumnsConstraintsTable) {
            this.verifyTable(SchemaVerifier.DataColumnConstraintsTableDefinition);
        }
    }

    /**
     * Requirement 62
     * <p>
     * <blockquote>
     * The {@code gpkg_data_column_constraints} table MAY be empty. If it
     * contains data, the lowercase {@code constraint_type} column values
     * SHALL be one of "range", "enum", or "glob".
     * </blockquote>
     *
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 62",
            text = "The gpkg_data_column_constraints table MAY be empty. "
                    + "If it contains data, the lowercase constraint_type "
                    + "column values SHALL be one of \"range\", \"enum\", or "
                    + "\"glob\". ")
    public void Requirement62() throws AssertionError {
        if (this.hasDataColumnsConstraintsTable) {
            final boolean validConstraintType = this.dataColumnConstraintsValues.stream()
                    .allMatch(dataColumnConstraintValue -> SchemaVerifier.isValidConstraintType(dataColumnConstraintValue.constraintType));

            Assert.assertTrue(String.format("There is(are) value(s) in %s table constraint_type that does not match \"range\" or \"enum\" or \"glob\". The invalid value(s): %s.",
                    GeoPackageSchema.DataColumnConstraintsTableName,
                    this.dataColumnConstraintsValues.stream()
                            .filter(dataColumnConstraintValue -> !SchemaVerifier.isValidConstraintType(dataColumnConstraintValue.constraintType))
                            .map(value -> value.constraintType).collect(Collectors.joining(", "))),
                    validConstraintType,
                    Severity.Warning);
        }
    }

    /**
     * Requirement 63
     * <p>
     * <blockquote>
     * gpkg_data_column_constraint constraint_name values for rows with
     * constraint_type values of <em>range</em> and <em>glob</em> SHALL be
     * unique.
     * </blockquote>
     *
     * @throws SQLException   throws if various SQLExceptions occur
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 63",
            text = "gpkg_data_column_constraint constraint_name values "
                    + "for rows with constraint_type values of range and "
                    + "glob SHALL be unique. ")
    public void Requirement63() throws SQLException, AssertionError {
        if (this.hasDataColumnsConstraintsTable) {
            final String query = String.format("SELECT DISTINCT constraint_name FROM %s WHERE constraint_type COLLATE NOCASE IN ('range', 'glob');",
                    GeoPackageSchema.DataColumnConstraintsTableName);

            final List<String> constraintNamesWithRangeOrGlob = JdbcUtility.select(this.getSqliteConnection(),
                    query,
                    null,
                    resultSet -> resultSet.getString(1));
            for (final String constraintName : constraintNamesWithRangeOrGlob) {
                final String query2 = String.format("SELECT count(*) FROM %s WHERE constraint_name = '?'",
                        GeoPackageSchema.DataColumnConstraintsTableName);

                try (final PreparedStatement statement2 = this.getSqliteConnection().prepareStatement(query2)) {
                    statement2.setString(1, constraintName);

                    try (final ResultSet countConstraintNameRS = statement2.executeQuery()) {
                        final int count = countConstraintNameRS.getInt("count(*)");

                        Assert.assertTrue(String.format("There are constraint_name values in %s with a constraint_type of 'glob' or 'range' are not unique. "
                                        + "Non-unique constraint_name: %s",
                                GeoPackageSchema.DataColumnConstraintsTableName,
                                constraintName),
                                count <= 1,
                                Severity.Warning);
                    }
                }
            }
        }
    }

    /**
     * Requirement 64
     * <p>
     * <blockquote>
     * The {@code gpkg_data_column_constraints} table MAY be empty. If it
     * contains rows with constraint_type column values of "range", the
     * {@code value} column values for those rows SHALL be NULL.
     * </blockquote>
     *
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 64",
            text = "The gpkg_data_column_constraints table MAY be empty. "
                    + "If it contains rows with constraint_type column "
                    + "values of \"range\", the value column values for "
                    + "those rows SHALL be NULL. ")
    public void Requirement64() throws AssertionError {
        if (this.hasDataColumnsConstraintsTable) {
            final List<DataColumnConstraints> invalidColumnConstraints = this.dataColumnConstraintsValues.stream()
                    .filter(dataColumnConstraint -> Type.Range.toString().equalsIgnoreCase(dataColumnConstraint.constraintType))
                    .filter(dataColumnConstraint -> dataColumnConstraint.value != null)
                    .collect(Collectors.toList());

            Assert.assertTrue(String.format("There are records in %s that have a constraint_type of \"range\" but does not have a corresponding null value for the column value. \nInvalid value(s): %s",
                    GeoPackageSchema.DataColumnConstraintsTableName,
                    invalidColumnConstraints.stream().map(columnValue -> columnValue.value).collect(Collectors.joining(", "))),
                    invalidColumnConstraints.isEmpty(),
                    Severity.Warning);
        }
    }

    /**
     * Requirement 65
     * <p>
     * <blockquote>
     * The {@code gpkg_data_column_constraints} table MAY be empty. If it
     * contains rows with {@code constraint_type} column values of
     * "range", the {@code min} column values for those rows SHALL be NOT
     * NULL and less than the {@code max} column value which shall be NOT
     * NULL.
     * </blockquote>
     *
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 65",
            text = "The gpkg_data_column_constraints table MAY be empty. If it contains rows with "
                    + "constraint_type column values of \"range\", the min column values for those "
                    + "rows SHALL be NOT NULL and less than the max column value which shall be NOT NULL.")
    public void Requirement65() throws AssertionError {
        if (this.hasDataColumnsConstraintsTable) {
            final List<DataColumnConstraints> invalidConstraintValuesWithRange = this.dataColumnConstraintsValues.stream()
                    .filter(constraintValue -> Type.Range.toString().equalsIgnoreCase(constraintValue.constraintType))
                    .filter(constraintValue -> constraintValue.min == null ||
                            constraintValue.max == null ||
                            constraintValue.min >= constraintValue.max)
                    .collect(Collectors.toList());

            Assert.assertTrue(String.format("The following records in %s have invalid values for min, or max or both:\n%s",
                    GeoPackageSchema.DataColumnConstraintsTableName,
                    invalidConstraintValuesWithRange.stream()
                            .map(DataColumnConstraints::invalidMinMaxWithRangeType)
                            .collect(Collectors.joining("\n"))),
                    invalidConstraintValuesWithRange.isEmpty(),
                    Severity.Warning);

        }
    }

    /**
     * Requirement 66
     * <p>
     * <blockquote>
     * The {@code gpkg_data_column_constraints} table MAY be empty. If it
     * contains rows with {@code constraint_type} column values of
     * "range", the {@code minIsInclusive} and {@code maxIsInclusive
     * } column values for those rows SHALL be 0 or 1.
     * </blockquote>
     *
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */

    @Requirement(reference = "Requirement 66",
            text = "The gpkg_data_column_constraints table MAY be empty. If it contains "
                    + "rows with constraint_type column values of \"range\", the minIsInclusive "
                    + "and maxIsInclusive column values for those rows SHALL be 0 or 1. ")
    public void Requirement66() throws AssertionError {
        if (this.hasDataColumnsConstraintsTable) {
            final List<DataColumnConstraints> invalidMinIsInclusives = this.dataColumnConstraintsValues.stream()
                    .filter(columnValue -> Type.Range.toString().equalsIgnoreCase(columnValue.constraintType))
                    .filter(columnValue -> !Boolean.TRUE.equals(columnValue.minIsInclusive) &&
                            !Boolean.FALSE.equals(columnValue.minIsInclusive))
                    .collect(Collectors.toList());

            final List<DataColumnConstraints> invalidMaxIsInclusives = this.dataColumnConstraintsValues.stream()
                    .filter(columnValue -> Type.Range.toString().equalsIgnoreCase(columnValue.constraintType))
                    .filter(columnValue -> !Boolean.TRUE.equals(columnValue.maxIsInclusive) &&
                            !Boolean.FALSE.equals(columnValue.maxIsInclusive))
                    .collect(Collectors.toList());

            Assert.assertTrue(String.format("The following are violations on either the minIsInclusive or maxIsIclusive columns "
                            + "in the %s table for which the values are not 0 or 1. %s. \n%s.",
                    GeoPackageSchema.DataColumnConstraintsTableName,
                    invalidMinIsInclusives.stream()
                            .map(record -> String.format("Invalid minIsInclusive for constraint_name: %10s.", record.constraintName))
                            .collect(Collectors.joining(", ")),
                    invalidMaxIsInclusives.stream()
                            .map(record -> String.format("Invalid maxIsInclusive for constraint_name: %10s.", record.constraintName))
                            .collect(Collectors.joining(", "))),
                    invalidMinIsInclusives.isEmpty() && invalidMaxIsInclusives.isEmpty(),
                    Severity.Warning);
        }
    }

    /**
     * Requirement 67
     * <p>
     * <blockquote>
     * The {@code gpkg_data_column_constraints} table MAY be empty. If it
     * contains rows with {@code constraint_type} column values of "enum"
     * or "glob", the {@code min}, {@code max}, {@code minIsInclusive} and
     * {@code maxIsInclusive} column values for those rows SHALL be NULL.
     * </blockquote>
     *
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 67",
            text = "The gpkg_data_column_constraints table MAY be empty. If it contains "
                    + "rows with constraint_type column values of \"enum\" or \"glob\", the min,"
                    + " max, minIsInclusive and maxIsInclusive column values for those rows SHALL be NULL.")
    public void Requirement67() throws AssertionError {
        if (this.hasDataColumnsConstraintsTable) {
            try {
                final List<DataColumnConstraints> invalidConstraintRecords = this.getDataColumnConstraintsValues()
                        .stream()
                        .filter(columnValue -> Type.Enum.toString().equalsIgnoreCase(columnValue.constraintType) ||
                                Type.Glob.toString().equalsIgnoreCase(columnValue.constraintType))
                        .filter(columnValue -> !(columnValue.min == null &&
                                columnValue.max == null &&
                                columnValue.minIsInclusive == null &&
                                columnValue.maxIsInclusive == null))
                        .collect(Collectors.toList());

                Assert.assertTrue(String.format("The following constraint_name(s) have a constraint_type of \"enum\" or \"glob\" "
                                + "and do NOT have null values for min, max, minIsInclusive, and/or maxIsInclusive. "
                                + "\nInvalid constraint_name(s): %s.",
                        invalidConstraintRecords.stream()
                                .map(columnValue -> columnValue.constraintName)
                                .collect(Collectors.joining(", "))),
                        invalidConstraintRecords.isEmpty(),
                        Severity.Warning);
            } catch (final SQLException ex) {
                Assert.assertTrue(ex.getMessage(),
                        true,
                        Severity.Error);
            }
        }
    }

    /**
     * Requirement 68
     * <p>
     * <blockquote>
     * The {@code gpkg_data_column_constraints} table MAY be empty. If it
     * contains rows with {@code constraint_type} column values of "enum"
     * or "glob", the {@code value} column SHALL NOT be NULL.
     * </blockquote>
     *
     * @throws AssertionError throws if the GeoPackage fails to meet this Requirement
     */
    @Requirement(reference = "Requirement 68",
            text = "The gpkg_data_column_constraints table MAY be empty. "
                    + "If it contains rows with constraint_type column values "
                    + "of \"enum\" or \"glob\", the value column SHALL NOT be NULL. ")
    public void Requirement68() throws AssertionError {
        if (this.hasDataColumnsConstraintsTable) {
            try {
                final List<DataColumnConstraints> invalidValues = this.getDataColumnConstraintsValues()
                        .stream()
                        .filter(columnValue -> Type.Enum.toString().equalsIgnoreCase(columnValue.constraintType) ||
                                Type.Glob.toString().equalsIgnoreCase(columnValue.constraintType))
                        .filter(columnValue -> columnValue.value == null)
                        .collect(Collectors.toList());

                Assert.assertTrue(String.format("The following constraint_name(s) from the %s table have invalid values for the column value. \nInvalid value with constraint_name as: %s.",
                        GeoPackageSchema.DataColumnConstraintsTableName,
                        invalidValues.stream()
                                .map(columnValue -> columnValue.constraintName)
                                .collect(Collectors.joining(", "))),
                        invalidValues.isEmpty(),
                        Severity.Warning);
            } catch (final SQLException ex) {
                Assert.assertTrue(ex.getMessage(),
                        true,
                        Severity.Error);
            }

        }
    }

    private static boolean isValidConstraintType(final String constraintType) {
        return Stream.of(Type.values()).anyMatch(scope -> scope.toString().equalsIgnoreCase(constraintType));
    }

    @SuppressWarnings("AssignmentToNull")
    private List<DataColumnConstraints> getDataColumnConstraintsValues() throws SQLException {
        final String query = String.format("SELECT constraint_name, constraint_type, value, min, minIsInclusive, max, maxIsInclusive FROM %s;", GeoPackageSchema.DataColumnConstraintsTableName);

        return JdbcUtility.select(this.getSqliteConnection(),
                query,
                null,
                resultSet -> {
                    final DataColumnConstraints dataColumnConstraints = new DataColumnConstraints();

                    dataColumnConstraints.constraintName = resultSet.getString("constraint_name");
                    dataColumnConstraints.constraintType = resultSet.getString("constraint_type");
                    dataColumnConstraints.value = resultSet.getString("value");
                    if (resultSet.wasNull()) {
                        dataColumnConstraints.value = null;
                    }
                    dataColumnConstraints.min = resultSet.getDouble("min");
                    if (resultSet.wasNull()) {
                        dataColumnConstraints.min = null;
                    }
                    dataColumnConstraints.minIsInclusive = resultSet.getBoolean("minIsInclusive");
                    if (resultSet.wasNull()) {
                        dataColumnConstraints.minIsInclusive = null;
                    }
                    dataColumnConstraints.max = resultSet.getDouble("max");
                    if (resultSet.wasNull()) {
                        dataColumnConstraints.max = null;
                    }
                    dataColumnConstraints.maxIsInclusive = resultSet.getBoolean("maxIsInclusive");
                    if (resultSet.wasNull()) {
                        dataColumnConstraints.maxIsInclusive = null;
                    }

                    return dataColumnConstraints;
                });
    }

    private List<DataColumns> getDataColumnValues() throws SQLException {
        final String query = String.format("SELECT table_name, column_name, constraint_name FROM %s;", GeoPackageSchema.DataColumnsTableName);

        return JdbcUtility.select(this.getSqliteConnection(),
                query,
                null,
                resultSet -> {
                    final DataColumns dataColumn = new DataColumns();

                    dataColumn.tableName = resultSet.getString("table_name");
                    dataColumn.columnName = resultSet.getString("column_name");
                    dataColumn.constraintName = resultSet.getString("constraint_name");

                    return dataColumn;
                });
    }

    private static final TableDefinition DataColumnsTableDefinition;
    private static final TableDefinition DataColumnConstraintsTableDefinition;

    static {
        final Map<String, ColumnDefinition> dataColumnsTableColumns = new HashMap<>();

        dataColumnsTableColumns.put("table_name", new ColumnDefinition("TEXT", true, true, true, null));
        dataColumnsTableColumns.put("column_name", new ColumnDefinition("TEXT", true, true, true, null));
        dataColumnsTableColumns.put("name", new ColumnDefinition("TEXT", false, false, false, null));
        dataColumnsTableColumns.put("title", new ColumnDefinition("TEXT", false, false, false, null));
        dataColumnsTableColumns.put("description", new ColumnDefinition("TEXT", false, false, false, null));
        dataColumnsTableColumns.put("mime_type", new ColumnDefinition("TEXT", false, false, false, null));
        dataColumnsTableColumns.put("constraint_name", new ColumnDefinition("TEXT", false, false, false, null));

        DataColumnsTableDefinition = new TableDefinition(GeoPackageSchema.DataColumnsTableName,
                dataColumnsTableColumns,
                new HashSet<>(Arrays.asList(new ForeignKeyDefinition(GeoPackageCore.ContentsTableName, "table_name", "table_name"))));


        final Map<String, ColumnDefinition> dataColumnConstraintsColumns = new HashMap<>();

        dataColumnConstraintsColumns.put("constraint_name", new ColumnDefinition("TEXT", true, false, false, null));
        dataColumnConstraintsColumns.put("constraint_type", new ColumnDefinition("TEXT", true, false, false, null));
        dataColumnConstraintsColumns.put("value", new ColumnDefinition("TEXT", false, false, false, null));
        dataColumnConstraintsColumns.put("min", new ColumnDefinition("NUMERIC", false, false, false, null));
        dataColumnConstraintsColumns.put("minIsInclusive", new ColumnDefinition("BOOLEAN", false, false, false, null));
        dataColumnConstraintsColumns.put("max", new ColumnDefinition("NUMERIC", false, false, false, null));
        dataColumnConstraintsColumns.put("maxIsInclusive", new ColumnDefinition("BOOLEAN", false, false, false, null));
        dataColumnConstraintsColumns.put("description", new ColumnDefinition("TEXT", false, false, false, null));

        DataColumnConstraintsTableDefinition = new TableDefinition(GeoPackageSchema.DataColumnConstraintsTableName,
                dataColumnConstraintsColumns,
                Collections.emptySet(),
                new HashSet<>(Arrays.asList(new UniqueDefinition("constraint_name", "constraint_type", "value"))));

    }
}
