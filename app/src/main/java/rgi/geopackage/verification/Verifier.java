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

package rgi.geopackage.verification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import rgi.common.Pair;
import rgi.common.util.jdbc.JdbcUtility;

import static com.rgi.geopackage.verification.Assert.assertTrue;
import static java.util.stream.Collectors.toMap;

/**
 * @author Luke Lambert
 * @author Jenifer Cochran
 */
public class Verifier {
    /**
     * Constructor
     *
     * @param verificationLevel Controls the level of verification testing performed
     * @param sqliteConnection  JDBC connection to the SQLite database
     */
    public Verifier(final Connection sqliteConnection, final VerificationLevel verificationLevel) {
        if (sqliteConnection == null) {
            throw new IllegalArgumentException("SQLite connection cannot be null");
        }

        this.sqliteConnection = sqliteConnection;
        this.verificationLevel = verificationLevel;
    }

    /**
     * Checks a GeoPackage (via it's {@link Connection}) for violations of the
     * requirements outlined in the <a href="http://www.geopackage.org/spec/">
     * standard</a>.
     *
     * @return Returns the definition for all failed requirements
     */
    public Collection<VerificationIssue> getVerificationIssues() {
        return this.getRequirements()
                .map(requirementTestMethod -> {
                    try {
                        requirementTestMethod.invoke(this);
                        return null;
                    } catch (final InvocationTargetException ex) {
                        final Requirement requirement = requirementTestMethod.getAnnotation(Requirement.class);

                        final Throwable cause = ex.getCause();

                        if (cause instanceof AssertionError) {
                            @SuppressWarnings("CastToConcreteClass") final AssertionError assertionError = (AssertionError) cause;

                            return assertionError.getSeverity() == Severity.Skipped ? null
                                    : new VerificationIssue(assertionError.getMessage(),
                                    requirement,
                                    assertionError.getSeverity());
                        }

                        return new VerificationIssue(String.format("Unexpected exception thrown when testing requirement %s for GeoPackage verification: %s",
                                requirement.reference(),
                                ex.getMessage()),
                                requirement);
                    } catch (final IllegalAccessException ex) {
                        // TODO
                        ex.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * @param dataType Data type type string
     * @return Returns true if dataType is one of the known SQL types or
     * matches one of the formatted TEXT or BLOB types
     */
    protected static boolean checkDataType(final String dataType) {
        return Verifier.AllowedSqlTypes.contains(dataType) ||
                dataType.matches("TEXT\\([0-9]+\\)") ||
                dataType.matches("BLOB\\([0-9]+\\)");
    }

    /**
     * @return Returns a stream of methods that are annotated with @Requirement
     */
    protected Stream<Method> getRequirements() {
        return Stream.of(this.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Requirement.class))
                .sorted(Comparator.comparing(method2 -> method2.getAnnotation(Requirement.class)
                                .reference(),
                        new LexicographicComparator()));
    }

    /**
     * @param table Table definition to verify
     * @throws AssertionError
     * @throws SQLException
     */
    protected void verifyTable(final TableDefinition table) throws AssertionError, SQLException {
        this.verifyTable(table.getName(),
                table.getColumns(),
                table.getForeignKeys(),
                table.getGroupUniques());
    }

    protected void verifyTable(final String tableName,
                               final Map<String, ColumnDefinition> expectedColumns,
                               final Set<ForeignKeyDefinition> expectedForeinKeys,
                               final Iterable<UniqueDefinition> expectedGroupUniques) throws AssertionError, SQLException {
        this.verifyTableDefinition(tableName);

        final Set<UniqueDefinition> uniques = this.getUniques(tableName);

        this.verifyColumns(tableName,
                expectedColumns,
                uniques);

        this.verifyForeignKeys(tableName,
                expectedForeinKeys);

        verifyGroupUniques(tableName,
                expectedGroupUniques,
                uniques);
    }

    protected void verifyTableDefinition(final String tableName) throws SQLException, AssertionError {
        final String sql = JdbcUtility.selectOne(this.sqliteConnection,
                "SELECT sql FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND tbl_name = ?;",
                preparedStatement -> preparedStatement.setString(1, tableName),
                resultSet -> resultSet.getString(1));

        assertTrue(String.format("The `sql` field must include the %s table SQL Definition.",
                tableName),
                sql != null,
                Severity.Error);
    }

    protected void verifyColumns(final String tableName, final Map<String, ColumnDefinition> requiredColumns, final Collection<UniqueDefinition> uniques) throws SQLException, AssertionError {
        final Map<String, ColumnDefinition> foundColumns = this.getColumnDefinitions(tableName, uniques);

        final Collection<String> errors = new LinkedList<>();
        final Collection<String> warnings = new LinkedList<>();

        // Make sure the required fields exist in the table
        for (final Map.Entry<String, ColumnDefinition> column : requiredColumns.entrySet()) {
            if (!foundColumns.containsKey(column.getKey())) {
                errors.add(String.format("required column: %s.%s is missing", tableName, column.getKey()));
                continue;
            }

            final ColumnDefinition columnDefinition = foundColumns.get(column.getKey());

            if (columnDefinition != null) {
                // .equals() for ColumnDefinition skips comparing default
                // values. It's better to check for functional equivalence
                // rather than exact string equality. This avoids issues with
                // difference in white space as well as other trivial
                // annoyances
                if (!columnDefinition.equals(column.getValue()) ||
                        !this.checkExpressionEquivalence(columnDefinition.getDefaultValue(),
                                column.getValue().getDefaultValue())) {
                    warnings.add(String.format("Required column %s is defined as:\n%s\nbut should be:\n%s",
                            column.getKey(),
                            columnDefinition.toString(),
                            column.getValue().toString()));
                }
            }
        }

        if (errors.isEmpty()) {
            assertTrue(String.format("Table %s doesn't match the expected table definition in the following ways:\n%s",
                    tableName,
                    String.join("\n", warnings)),
                    warnings.isEmpty(),
                    Severity.Warning);
        } else {
            errors.addAll(warnings);
            assertTrue(String.format("Table %s doesn't match the expected table definition in the following ways:\n%s",
                    tableName,
                    String.join("\n", errors)),
                    errors.isEmpty(),
                    Severity.Error);
        }
    }

    protected void verifyForeignKeys(final String tableName, final Set<ForeignKeyDefinition> requiredForeignKeys) throws AssertionError, SQLException {
        try (final Statement statement = this.sqliteConnection.createStatement()) {
            try (final ResultSet fkInfo = statement.executeQuery(String.format("PRAGMA foreign_key_list(%s);", tableName))) {
                if (fkInfo.isClosed()) {
                    // TODO
                    // There seems to be an issue with this SQLite driver when
                    // PRAGMA foreign_key_list would return no results. Instead
                    // of being empty, the ResultSet itself is closed which
                    // causes JdbcUtility to complain loudly.
                    return;
                }

                final List<ForeignKeyDefinition> foundForeignKeys = JdbcUtility.map(fkInfo,
                        resultSet -> new ForeignKeyDefinition(resultSet.getString("table"),
                                resultSet.getString("from"),
                                resultSet.getString("to")));

                final Collection<ForeignKeyDefinition> missingKeys = new HashSet<>(requiredForeignKeys);
                missingKeys.removeAll(foundForeignKeys);

                final Collection<ForeignKeyDefinition> extraneousKeys = new HashSet<>(foundForeignKeys);
                extraneousKeys.removeAll(requiredForeignKeys);

                final StringBuilder error = new StringBuilder();

                if (!missingKeys.isEmpty()) {
                    error.append(String.format("The table %s is missing the foreign key constraint(s): \n", tableName));
                    for (final ForeignKeyDefinition key : missingKeys) {
                        error.append(String.format("%s.%s -> %s.%s\n",
                                tableName,
                                key.getFromColumnName(),
                                key.getReferenceTableName(),
                                key.getToColumnName()));
                    }
                }

                if (!extraneousKeys.isEmpty()) {
                    error.append(String.format("The table %s has extraneous foreign key constraint(s): \n", tableName));
                    for (final ForeignKeyDefinition key : extraneousKeys) {
                        error.append(String.format("%s.%s -> %s.%s\n",
                                tableName,
                                key.getFromColumnName(),
                                key.getReferenceTableName(),
                                key.getToColumnName()));
                    }
                }

                assertTrue(error.toString(),
                        error.length() == 0,
                        Severity.Error);
            }
        }
    }

    protected static void verifyGroupUniques(final String tableName,
                                             final Iterable<UniqueDefinition> requiredGroupUniques,
                                             final Collection<UniqueDefinition> uniques) throws AssertionError {
        for (final UniqueDefinition groupUnique : requiredGroupUniques) {
            assertTrue(String.format("The table %s is missing the column group unique constraint: (%s)",
                    tableName,
                    String.join(", ", groupUnique.getColumnNames())),
                    uniques.contains(groupUnique),
                    Severity.Error);
        }
    }

    protected Set<UniqueDefinition> getUniques(final String tableName) throws SQLException {
        final Set<UniqueDefinition> uniqueDefinitions = new HashSet<>();

        final Collection<String> indexNames = JdbcUtility.filterSelect(this.sqliteConnection,
                String.format("PRAGMA index_list(%s);", tableName),
                null,
                resultSet -> resultSet.getBoolean("unique"),
                resultSet -> resultSet.getString("name"));

        for (final String indexName : indexNames) {
            uniqueDefinitions.add(new UniqueDefinition(JdbcUtility.select(this.sqliteConnection,
                    String.format("PRAGMA index_info(%s);", indexName),
                    null,
                    resultSet -> resultSet.getString("name"))));
        }

        return uniqueDefinitions;
    }

    /**
     * @return The SQLite connection
     */
    protected Connection getSqliteConnection() {
        return this.sqliteConnection;
    }

    /**
     * @return The list of allowed SQL types
     */
    protected static List<String> getAllowedSqlTypes() {
        return Collections.unmodifiableList(Verifier.AllowedSqlTypes);
    }


    private Map<String, ColumnDefinition> getColumnDefinitions(final String tableName, final Collection<UniqueDefinition> uniques) throws SQLException {
        return JdbcUtility.select(this.sqliteConnection,
                String.format("PRAGMA table_info(%s);", tableName),
                null,
                resultSet -> {
                    final String columnName = resultSet.getString("name");
                    return Pair.of(columnName,
                            new ColumnDefinition(resultSet.getString("type"),
                                    resultSet.getBoolean("notnull"),
                                    resultSet.getBoolean("pk"),
                                    uniques.stream().anyMatch(unique -> unique.equals(columnName)),
                                    resultSet.getString("dflt_value")));
                })
                .stream()
                .collect(toMap(Pair::getLeft,
                        Pair::getRight));
    }

    private boolean checkExpressionEquivalence(final String expression1,
                                               final String expression2) throws SQLException {
        if ((expression1 == null) || (expression2 == null)) {
            return (expression1 == null) && (expression2 == null);
        }

        try (final Statement statement = this.sqliteConnection.createStatement()) {
            final String query = String.format("SELECT (%s) = (%s);",
                    expression1,
                    expression2);

            try (final ResultSet results = statement.executeQuery(query)) {
                return results.next() && results.getBoolean(1);
            }
        }
    }

    private final Connection sqliteConnection;

    protected final VerificationLevel verificationLevel;

    private static final List<String> AllowedSqlTypes = Arrays.asList("BOOLEAN", "TINYINT", "SMALLINT", "MEDIUMINT",
            "INT", "FLOAT", "DOUBLE", "REAL",
            "TEXT", "BLOB", "DATE", "DATETIME",
            "GEOMETRY", "POINT", "LINESTRING", "POLYGON",
            "MULTIPOINT", "MULTILINESTRING", "MULTIPOLYGON", "GEOMETRYCOLLECTION",
            "INTEGER");
}
