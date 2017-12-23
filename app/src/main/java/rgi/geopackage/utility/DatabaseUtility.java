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

package rgi.geopackage.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import rgi.common.util.jdbc.JdbcUtility;

/**
 * @author Luke Lambert
 */
public final class DatabaseUtility {
    private DatabaseUtility() {
    }

    /**
     * @param sqliteFile the GeoPackage file
     * @return the String representation of the sqliteVersion
     * @throws IOException throws when the FileDoes not exist or unable to seek in the
     *                     File to read the SQLite Version
     */
    public static DatabaseVersion getSqliteVersion(final File sqliteFile) throws IOException {
        if (sqliteFile == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (!sqliteFile.exists()) {
            throw new FileNotFoundException("File does not exist: " + sqliteFile.getPath());
        }

        if (sqliteFile.length() < 100) {
            throw new IllegalArgumentException("File must be at least 100 bytes to be an SQLite file.");
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(sqliteFile, "r")) {
            // https://www.sqlite.org/fileformat2.html
            // Bytes 96 -> 100 are an int representing the sqlite version
            final int databaseVersionByteOffset = 96;

            randomAccessFile.seek(databaseVersionByteOffset);
            final int version = randomAccessFile.readInt();

            // Major/minor/revision, https://www.sqlite.org/fileformat2.html
            final int major = version / 1000000;
            final int minor = (version - (major * 1000000)) / 1000;
            final int revision = version - ((major * 1000000) + (minor * 1000));

            return new DatabaseVersion(major, minor, revision);
        }
    }

    /**
     * @param connection the connection to the database
     * @param name       the name of the table
     * @return true if the table or view exists in the database; otherwise
     * returns false
     * @throws SQLException throws if unable to connect to the database or other various
     *                      SQLExceptions
     */
    public static boolean doesTableOrViewExists(final Connection connection, final String name) throws SQLException {
        DatabaseUtility.verify(connection);

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Table/view name cannot be null or empty");
        }

        //noinspection ConstantConditions // selecting a count will always produce a result
        return JdbcUtility.selectOne(connection,
                "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? COLLATE NOCASE LIMIT 1;",
                preparedStatement -> preparedStatement.setString(1, name),
                resultSet -> resultSet.getInt(1)) > 0;
    }

    /**
     * @param connection the connection to the database
     * @param names      the names of the tables
     * @return true if All the tables or views exists in the database; otherwise
     * returns false
     * @throws SQLException throws if unable to connect to the database or other various
     *                      SQLExceptions
     */
    public static boolean doTablesOrViewsExists(final Connection connection, final String... names) throws SQLException {
        DatabaseUtility.verify(connection);

        final Collection<String> uniqueNames = new HashSet<>(Arrays.asList(names));

        //noinspection ConstantConditions // selecting a count will always produce a result
        return JdbcUtility.selectOne(connection,
                String.format("SELECT COUNT(*) AS count FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name COLLATE NOCASE IN (%s);",
                        String.join(", ", Collections.nCopies(uniqueNames.size(), "?"))),
                preparedStatement -> {
                    int index = 1;

                    for (final String name : uniqueNames) {
                        preparedStatement.setString(index++, name);
                    }
                },
                resultSet -> resultSet.getInt("count")) == uniqueNames.size();
    }

    /**
     * @param connection    connection to the database
     * @param applicationId the int Application ID to be set
     * @throws SQLException throws if various SQLExceptions occur
     */
    public static void setApplicationId(final Connection connection, final int applicationId) throws SQLException {
        DatabaseUtility.verify(connection);

        JdbcUtility.update(connection, String.format("PRAGMA application_id = %d;", applicationId));
    }

    /**
     * @param connection connection to the database
     * @return the application Id of the database
     * @throws SQLException throws if various SQLExceptions occur
     */
    public static int getApplicationId(final Connection connection) throws SQLException {
        DatabaseUtility.verify(connection);

        //noinspection ConstantConditions   // PRAGMA commands should always return a value
        return JdbcUtility.selectOne(connection,
                "PRAGMA application_id;",
                null,
                resultSet -> resultSet.getInt("application_id"));
    }

    /**
     * @param connection  connection to the database
     * @param userVersion application user version, in this case the GeoPackage version
     * @throws SQLException throws if various SQLExceptions occur
     */
    public static void setUserVersion(final Connection connection, final int userVersion) throws SQLException {
        DatabaseUtility.verify(connection);

        JdbcUtility.update(connection, String.format("PRAGMA user_version = %d;", userVersion));
    }

    /**
     * @param connection connection to the database
     * @return the GeoPackage version
     * @throws SQLException throws if various SQLExceptions occur
     */
    public static int getUserVersion(final Connection connection) throws SQLException {
        DatabaseUtility.verify(connection);

        //noinspection ConstantConditions   // PRAGMA commands should always return a value
        return JdbcUtility.selectOne(connection,
                "PRAGMA user_version;",
                null,
                resultSet -> resultSet.getInt("user_version"));
    }

    /**
     * @param connection connection to the database
     * @param state      true or false whether you want foreign_keys to be set on or
     *                   off
     * @throws SQLException throws if various SQLExceptions occur
     */
    public static void setPragmaForeignKeys(final Connection connection,
                                            final ToggleState state) throws SQLException {
        DatabaseUtility.verify(connection);

        JdbcUtility.update(connection,
                String.format("PRAGMA foreign_keys = %d;",
                        state.getState()));
    }

    /**
     * @param connection connection to the database
     * @param mode       value to set the SQLite journal_mode PRAGMA
     * @throws SQLException throws if various SQLExceptions occur
     */
    public static void setPragmaJournalMode(final Connection connection,
                                            final JournalMode mode) throws SQLException {
        DatabaseUtility.verify(connection);

        JdbcUtility.update(connection,
                String.format("PRAGMA journal_mode = %s;",
                        mode.name()));
    }

    /**
     * @param connection connection to the database
     * @param state      State to set the 'synchronous' PRAGMA to
     * @throws SQLException throws if various SQLExceptions occur
     */
    public static void setPragmaSynchronous(final Connection connection, final SynchronousState state) throws SQLException {
        DatabaseUtility.verify(connection);

        JdbcUtility.update(connection,
                String.format("PRAGMA synchronous = %d;",
                        state.getState()));
    }

    /**
     * Get the smallest value for a table and column <i>that does not yet exist
     * </i>
     *
     * @param connection connection to the database
     * @param tableName  table name
     * @param columnName column name
     * @return the smallest value for a table and column that does not yet exist
     * @throws SQLException if there's a database error
     */
    public static <T> T nextValue(final Connection connection, final String tableName, final String columnName) throws SQLException {
        final String smallestNonexistentValue = String.format("SELECT (table1.%1$s + 1) " +
                        "FROM %2$s AS table1 LEFT JOIN %2$s table2 on table2.%1$s = (table1.%1$s + 1) " +
                        "WHERE table2.%1$s IS NULL " +
                        "ORDER BY table1.%1$s " +
                        "LIMIT 1",
                columnName,
                tableName);
        //noinspection unchecked    Cast is intentional, since the ResultSet class has no generic getter
        return JdbcUtility.selectOne(connection,
                smallestNonexistentValue,
                null,
                resultSet -> (T) resultSet.getObject(1));
    }

    /**
     * Gets the column names of a table or view.
     *
     * @param connection      connection to the database
     * @param tableOrViewName The name of the table or view to be queried
     * @return a List of column names
     * @throws SQLException if there's a database error
     */
    public static List<String> getColumnNames(final Connection connection, final String tableOrViewName) throws SQLException {
        verify(connection);

        if (tableOrViewName == null || tableOrViewName.isEmpty()) {
            throw new IllegalArgumentException("Table or view name may not be null or empty");
        }

        return JdbcUtility.select(connection,
                String.format("PRAGMA table_info('%s')", tableOrViewName),
                null,
                resultSet -> resultSet.getString("name"));
    }

    /**
     * Ensures that a table or view name is valid according to the GeoPackage
     * spec
     *
     * @param tableName name of the table or view
     */
    public static void validateTableName(final String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("The table name may not be null or empty");
        }

        if (sqlKeywords.stream()
                .anyMatch(keyword -> keyword.equalsIgnoreCase(tableName))) {
            throw new IllegalArgumentException(String.format("The table name may not be an SQL keyword: %s", String.join(", ", sqlKeywords)));
        }

        if (!identifierPattern.matcher(tableName).matches()) {
            throw new IllegalArgumentException("The table name must begin with a letter (A..Z, a..z) or an underscore (_) and may only be followed by letters, underscores, or numbers");
        }

        if (tableName.toLowerCase().startsWith("gpkg_")) {
            throw new IllegalArgumentException("The table name may not start with the reserved prefix 'gpkg_'");
        }
    }

    /**
     * Ensures that the column name is valid according to the GeoPackage spec
     *
     * @param columnName name of the column
     */
    public static void validateColumnName(final String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            throw new IllegalArgumentException("The column name may not be null or empty");
        }

        if (sqlKeywords.stream()
                .anyMatch(keyword -> keyword.equalsIgnoreCase(columnName))) {
            throw new IllegalArgumentException(String.format("The column name may not be an SQL keyword: %s", String.join(", ", sqlKeywords)));
        }

        // TODO
        // By wrapping column names in double quotes, you can get away with
        // a variety of strange names. This is *strongly* discouraged, but I'm
        // not sure it's correct to explicitly forbid it.
        if (!identifierPattern.matcher(columnName).matches()) {
            // This is just a best guess. SQLite doesn't actually have an EBNF diagram for "column-name"
            throw new IllegalArgumentException(String.format("The column '%s' must begin with a letter (A..Z, a..z) or an underscore (_) and may only be followed by letters, underscores, or numbers.",
                    columnName));
        }
    }

    private static void verify(final Connection connection) throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new IllegalArgumentException("The connection cannot be null or closed.");
        }
    }

    private static final Pattern identifierPattern = Pattern.compile("^[_a-zA-Z]\\w*"); // This is just a best guess. SQLite doesn't actually have an EBNF diagram for "column-name"

    private static final Collection<String> sqlKeywords = Arrays.asList("ABORT", "ACTION", "ADD", "AFTER", "ALL",
            "ALTER", "ANALYZE", "AND", "AS", "ASC",
            "ATTACH", "AUTOINCREMENT", "BEFORE", "BEGIN", "BETWEEN",
            "BY", "CASCADE", "CASE", "CAST", "CHECK",
            "COLLATE", "COLUMN", "COMMIT", "CONFLICT", "CONSTRAINT",
            "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
            "DATABASE", "DEFAULT", "DEFERRABLE", "DEFERRED", "DELETE",
            "DESC", "DETACH", "DISTINCT", "DROP", "EACH",
            "ELSE", "END", "ESCAPE", "EXCEPT", "EXCLUSIVE",
            "EXISTS", "EXPLAIN", "FAIL", "FOR", "FOREIGN",
            "FROM", "FULL", "GLOB", "GROUP", "HAVING",
            "IF", "IGNORE", "IMMEDIATE", "IN", "INDEX",
            "INDEXED", "INITIALLY", "INNER", "INSERT", "INSTEAD",
            "INTERSECT", "INTO", "IS", "ISNULL", "JOIN",
            "KEY", "LEFT", "LIKE", "LIMIT", "MATCH",
            "NATURAL", "NO", "NOT", "NOTNULL", "NULL",
            "OF", "OFFSET", "ON", "OR", "ORDER",
            "OUTER", "PLAN", "PRAGMA", "PRIMARY", "QUERY",
            "RAISE", "RECURSIVE", "REFERENCES", "REGEXP", "REINDEX",
            "RELEASE", "RENAME", "REPLACE", "RESTRICT", "RIGHT",
            "ROLLBACK", "ROW", "SAVEPOINT", "SELECT", "SET",
            "TABLE", "TEMP", "TEMPORARY", "THEN", "TO",
            "TRANSACTION", "TRIGGER", "UNION", "UNIQUE", "UPDATE",
            "USING", "VACUUM", "VALUES", "VIEW", "VIRTUAL",
            "WHEN", "WHERE", "WITH", "WITHOUT");
}
