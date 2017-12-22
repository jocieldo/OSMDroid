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

package com.rgi.geopackage.core;

import com.rgi.common.Pair;
import com.rgi.common.util.jdbc.JdbcUtility;
import com.rgi.geopackage.features.GeoPackageFeatures;
import com.rgi.geopackage.tiles.GeoPackageTiles;
import com.rgi.geopackage.utility.DatabaseUtility;
import com.rgi.geopackage.verification.AssertionError;
import com.rgi.geopackage.verification.ColumnDefinition;
import com.rgi.geopackage.verification.ForeignKeyDefinition;
import com.rgi.geopackage.verification.Requirement;
import com.rgi.geopackage.verification.Severity;
import com.rgi.geopackage.verification.TableDefinition;
import com.rgi.geopackage.verification.VerificationLevel;
import com.rgi.geopackage.verification.Verifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.rgi.geopackage.verification.Assert.assertArrayEquals;
import static com.rgi.geopackage.verification.Assert.assertTrue;
import static com.rgi.geopackage.verification.Assert.fail;

/**
 * Verifier for the GeoPackage Core requirements
 *
 * @author Luke Lambert
 * @author Jenifer Cochran
 */
@SuppressWarnings("JDBCExecuteWithNonConstantString")
public class CoreVerifier extends Verifier {
    private boolean hasContentsTable;
    private boolean hasSpatialReferenceSystemTable;

    /**
     * Constructor
     *
     * @param file              File handle to the SQLite database
     * @param sqliteConnection  JDBC connection to the SQLite database
     * @param verificationLevel Controls the level of verification testing performed
     */
    public CoreVerifier(final File file, final Connection sqliteConnection, final VerificationLevel verificationLevel) {
        super(sqliteConnection, verificationLevel);

        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        this.file = file;

        try {
            this.hasContentsTable = this.doesTableExist(GeoPackageCore.ContentsTableName);
        } catch (final SQLException ignored) {
            this.hasContentsTable = false;
        }

        try {
            this.hasSpatialReferenceSystemTable = this.doesTableExist(GeoPackageCore.SpatialRefSysTableName);
        } catch (final SQLException ignored) {
            this.hasSpatialReferenceSystemTable = false;
        }
    }

    /**
     * Requirement 1
     * <p>
     * <blockquote>
     * A GeoPackage SHALL be a <a href="http://www.sqlite.org/">SQLite</a>
     * database file using <a href="http://sqlite.org/fileformat2.html">version
     * 3 of the SQLite file format</a>. The first 16 bytes of a GeoPackage
     * SHALL be the null-terminated <a href="http://www.geopackage.org/spec/#B4"
     * >ASCII</a> string "SQLite format 3".
     * </blockquote>
     *
     * @throws IOException    throws if an I/O error occurs when reading the file header
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 1",
            text = "A GeoPackage SHALL be a SQLite database file using version 3 of the SQLite file format. The first 16 bytes of a GeoPackage SHALL be the null-terminated ASCII string \"SQLite format 3\".")
    public void requirement1() throws IOException, AssertionError {
        final byte[] header = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);    // The GeoPackage spec says it's StandardCharsets.US_ASCII, but the SQLite spec (https://www.sqlite.org/fileformat.html - 1.2.1 Magic Header String) says it's UTF8, i.e, StandardCharsets.UTF_8

        final byte[] data = new byte[header.length];

        try (final FileInputStream fileInputStream = new FileInputStream(this.file)) {
            assertTrue("The header information of the file does not contain enough bytes to include necessary information",
                    fileInputStream.read(data, 0, header.length) == header.length,
                    Severity.Error);

            assertArrayEquals("The database file is not using a version 3 of the SQLite format.  Or does not include the SQLite version in the file header.",
                    header,
                    data,
                    Severity.Warning);
        }
    }

    /**
     * Requirement 2
     * <p>
     * <blockquote>
     * A GeoPackage SHALL contain a value of 0x47504B47 ("GPKG" in ASCII) in the
     * "application_id" field of the SQLite database header to indicate that it
     * is a GeoPackage. A GeoPackage SHALL contain an appropriate value in
     * "user_version" field of the SQLite database header to indicate its
     * version. The value SHALL be in integer with a major version, two-digit
     * minor version, and two-digit bug-fix. For GeoPackage Version 1.2 this
     * value is 0x000027D8 (the hexadecimal value for 10200).
     * </blockquote>
     *
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 2",
            text = "A GeoPackage SHALL contain a value of 0x47504B47 (\"GPKG\" in ASCII) in the \"application_id\" field of the SQLite database header to indicate that it is a GeoPackage. A GeoPackage SHALL contain an appropriate value in \"user_version\" field of the SQLite database header to indicate its version. The value SHALL be in integer with a major version, two-digit minor version, and two-digit bug-fix. For GeoPackage Version 1.2 this value is 0x000027D8 (the hexadecimal value for 10200).")
    public void requirement2() throws AssertionError {
        final int byteSizeOfApplicationId = 4;
        final byte[] applicationId = new byte[byteSizeOfApplicationId];

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(this.file, "r")) {
            final long applicationUserVersionByteOffset = 60; // https://sqlite.org/fileformat2.html#database_header
            randomAccessFile.seek(applicationUserVersionByteOffset);

            final int byteSizeOfUserVersion = 4;
            final byte[] userVersion = new byte[byteSizeOfUserVersion];

            assertTrue("The file does not have enough bytes to contain a GeoPackage.",
                    randomAccessFile.read(userVersion,
                            0,
                            byteSizeOfUserVersion) == byteSizeOfUserVersion,
                    Severity.Error);


            final long applicationIdByteOffset = 68; // https://sqlite.org/fileformat2.html#database_header
            randomAccessFile.seek(applicationIdByteOffset);
            assertTrue("The file does not have enough bytes to contain a GeoPackage.",
                    randomAccessFile.read(applicationId,
                            0,
                            byteSizeOfApplicationId) == byteSizeOfApplicationId,
                    Severity.Error);
        } catch (final Exception ex) {
            throw new AssertionError(ex, Severity.Error);
        }

        final byte[] expectedApplicationId = {(byte) 'G', (byte) 'P', (byte) 'K', (byte) 'G'};

        assertArrayEquals(String.format("Bad Application ID prefix: \"%s\", expected: \"%s\"",
                new String(applicationId),
                new String(expectedApplicationId)),
                expectedApplicationId,
                applicationId,
                Severity.Warning);

        // There's no test for user_version - the requirement's only constraint
        // is that it's 4 bytes (int). The requirement also specifies how the
        // values are interpreted, but doesn't actually constrain them.
    }

    /**
     * Requirement 3
     * <p>
     * <blockquote>
     * A GeoPackage SHALL have the file extension name ".gpkg".
     * </blockquote>
     *
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 3",
            text = "A GeoPackage SHALL have the file extension name \".gpkg\".")
    public void requirement3() throws AssertionError {
        assertTrue("Expected a file with the extension: '.gpkg'",
                this.file.getName().endsWith(".gpkg"),
                Severity.Warning);
    }

    /**
     * Requirement 4
     * <p>
     * <blockquote>
     * A GeoPackage SHALL only contain data elements, SQL constructs and
     * GeoPackage extensions with the "gpkg" author name specified in this
     * encoding standard.
     * </blockquote>
     */
    @Requirement(reference = "Requirement 4",
            text = "A GeoPackage SHALL only contain data elements, SQL constructs and GeoPackage extensions with the \"gpkg\" author name specified in this encoding standard.")
    public static void requirement4() {
        // This requirement is tested through other test cases.
        // The tables we test are:
        // gpkg_contents            per test Req13 in CoreVerifier
        // gpkg_spatial_ref_sys     per test Req10 in CoreVerifier
        // gpkg_tile_matrix         per test Req41 in TileVerifier
        // gpkg_tile_matrix_set     per test Req37 in TileVerifier
        // Pyramid User Data Tables per test Req33 in TileVerifier
    }

    /**
     * Requirement 5
     * <p>
     * <blockquote>
     * The columns of tables in a GeoPackage SHALL only be declared using one of
     * the data types specified in table <a href=
     * "http://www.geopackage.org/spec/#table_column_data_types">GeoPackage Data
     * Types</a>.
     * </blockquote>
     *
     * @throws SQLException   throws if an SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 5",
            text = "The columns of tables in a GeoPackage SHALL only be declared using one of the data types specified in table GeoPackage Data Types.")
    public void requirement5() throws SQLException, AssertionError {
        if (this.hasContentsTable) {
            final String query = String.format("SELECT DISTINCT table_name\n" +
                            "FROM %s\n" +
                            "WHERE table_name COLLATE NOCASE IN\n" +
                            "    (SELECT name FROM sqlite_master\n" +
                            "     WHERE (type = 'table' OR\n" +
                            "            type = 'view') AND\n" +
                            "           name = table_name COLLATE NOCASE);",
                    GeoPackageCore.ContentsTableName);

            final List<String> validContentsTableTableNames = JdbcUtility.select(this.getSqliteConnection(),
                    query,
                    null,
                    result -> result.getString(1));

            final Map<String, List<Pair<String, String>>> tablesWithColumnsWithBadTypes = new HashMap<>();

            for (final String tableName : validContentsTableTableNames) {
                final String tableInfoQuery = String.format("PRAGMA table_info('%s');", tableName);

                final List<Pair<String, String>> columnsWithBadDataTypes = JdbcUtility.selectFilter(this.getSqliteConnection(),
                        tableInfoQuery,
                        null,
                        resultSet -> Pair.of(resultSet.getString("name"),
                                resultSet.getString("type")),
                        pair -> !Verifier.checkDataType(pair.getRight()));

                if (!columnsWithBadDataTypes.isEmpty()) {
                    tablesWithColumnsWithBadTypes.put(tableName, columnsWithBadDataTypes);
                }
            }

            assertTrue(String.format("The following tables have columns with the incorrect type:\n%s",
                    String.join("\n",
                            tablesWithColumnsWithBadTypes.entrySet()
                                    .stream()
                                    .map(entry -> String.format("%s: %s",
                                            entry.getKey(),
                                            String.join(", ",
                                                    entry.getValue()
                                                            .stream()
                                                            .map(column -> String.format("%s (%s)",
                                                                    column.getLeft(),
                                                                    column.getRight()))
                                                            .collect(Collectors.toList()))))
                                    .collect(Collectors.toList()))),
                    tablesWithColumnsWithBadTypes.isEmpty(),
                    Severity.Error);
        }
    }

    /**
     * Requirement 6
     * <p>
     * <blockquote>
     * The SQLite PRAGMA integrity_check SQL command SHALL return "ok" for a
     * GeoPackage file.
     * </blockquote>
     *
     * @throws SQLException   throws if SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 6",
            text = "The SQLite PRAGMA integrity_check SQL command SHALL return \"ok\" for a GeoPackage file.")
    public void requirement6() throws SQLException, AssertionError {
        assertTrue("Test skipped when verification level is not set to " + VerificationLevel.Full,
                this.verificationLevel == VerificationLevel.Full,
                Severity.Skipped);

        final String integrityCheck = JdbcUtility.selectOne(this.getSqliteConnection(),
                "PRAGMA integrity_check;",
                null,
                resultSet -> resultSet.getString("integrity_check"));

        assertTrue("PRAGMA integrity_check failed.",
                integrityCheck != null && integrityCheck.toLowerCase().equals("ok"),
                Severity.Error);
    }

    /**
     * Requirement 7
     * <p>
     * <blockquote>
     * The SQLite PRAGMA foreign_key_check SQL with no parameter value SHALL
     * return an empty result set indicating no invalid foreign key values for a
     * GeoPackage file.
     * </blockquote>
     *
     * @throws SQLException   throws if SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 7",
            text = "The SQLite PRAGMA foreign_key_check SQL with no parameter value SHALL return an empty result set indicating no invalid foreign key values for a GeoPackage file.")
    public void requirement7() throws SQLException, AssertionError {
        final List<String> foreignKeyCheck = JdbcUtility.select(this.getSqliteConnection(),
                "PRAGMA foreign_key_check;",
                null,
                resultSet -> "");


        assertTrue("PRAGMA foreign_key_check failed.",
                foreignKeyCheck.isEmpty(),
                Severity.Error);
    }

    /**
     * Requirement 8
     * <p>
     * <blockquote>
     * A GeoPackage SQLite Configuration SHALL provide SQL access to GeoPackage
     * contents via <a href="">SQLite version 3</a>software APIs.
     * </blockquote>
     *
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 8",
            text = "A GeoPackage SQLite Configuration SHALL provide SQL access to GeoPackage contents via SQLite version 3 software APIs.")
    public void requirement8() throws AssertionError {
        try {
            JdbcUtility.selectOne(this.getSqliteConnection(),
                    "SELECT * FROM sqlite_master;",
                    null,
                    resultSet -> resultSet);
        } catch (final SQLException ignored) {
            fail("GeoPackage needs to provide the SQLite SQL API interface.",
                    Severity.Error);
        }
    }

    /**
     * Requirement 9
     * <p>
     * <blockquote>
     * The text of Requirement 9 has been struck out.
     * </blockquote>
     *
     * @throws SQLException   throws if SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 9",
            text = "")
    public void requirement9() throws SQLException, AssertionError {
        // The text of Requirement 9 has been struck out.
    }

    /**
     * Requirement 10
     * <p>
     * <blockquote>
     * A GeoPackage SHALL include a {@code gpkg_spatial_ref_sys} table per
     * clause 1.1.2.1.1 <a href=
     * "http://www.geopackage.org/spec/#spatial_ref_sys_data_table_definition">
     * Table Definition</a>, Table <a href=
     * "http://www.geopackage.org/spec/#gpkg_spatial_ref_sys_cols">Spatial Ref
     * Sys Table Definition</a> and Table <a href=
     * "http://www.geopackage.org/spec/#gpkg_spatial_ref_sys_sql">
     * gpkg_spatial_ref_sys Table Definition SQL</a>.
     * </blockquote>
     *
     * @throws SQLException   throws if SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 10",
            text = "A GeoPackage SHALL include a gpkg_spatial_ref_sys table per clause 1.1.2.1.1 Table Definition, Table Spatial Ref Sys Table Definition and Table gpkg_spatial_ref_sys Table Definition SQL.")
    public void requirement10() throws AssertionError, SQLException {
        if (this.hasSpatialReferenceSystemTable) {
            this.verifyTable(CoreVerifier.SpatialReferenceSystemDefinition);
        } else {
            throw new AssertionError(String.format("The GeoPackage does not contain a %s table. This is a required table for all GeoPackages.",
                    GeoPackageCore.SpatialRefSysTableName),
                    Severity.Error);
        }
    }

    // TODO

    // 1.1.2.1.2. Table Data Values
    // Definition column WKT values in the <code>gpkg_spatial_ref_sys</code>
    // table SHALL define the Spatial Reference Systems used by feature
    // geometries and tile images, unless these SRS are unknown and therefore
    // undefined as specified in <a href="#_requirement-11">[_requirement-11]
    // </a>. Values SHALL be constructed per the EBNF syntax in <a href="#32">
    // [32]</a> clause 7. EBNF name and number values MAY be obtained from any
    // specified authority, e.g. <a href="#13">[13]</a><a href="#14">[14]</a>.
    // For example, see the return value in
    // <a href="#spatial_ref_sys_data_values_default">
    // [spatial_ref_sys_data_values_default]</a> Test Method step (3) used to
    // test the definition for WGS-84 per <a href="#_requirement-11">
    // [_requirement-11]</a>:</p>

    // I don't know how the first SHALL can be tested beyond what requirement
    // 12 already specifies. It may imply that the srs_id in the binary header
    // of a feature blob must match (which doesn't appear to be specified
    // elsewhere) but this paragraph seems like a very strange place to indicate
    // that.

    // The second SHALL requires some tool to parse the WTK EBNF for SRS
    // definitions (e.g. ANTRL), and check the contents against that. That's not
    // a trivial thing.

    /**
     * Requirement 11
     * <p>
     * The {@code gpkg_spatial_ref_sys} table SHALL contain at a minimum the
     * records listed in <a href=
     * "http://www.geopackage.org/spec/#gpkg_spatial_ref_sys_records">Spatial
     * Ref Sys Table Records</a>. The record with an {@code srs_id} of 4326
     * SHALL correspond to <a href="http://www.google.com/search?as_q=WGS-84">
     * WGS-84</a> as defined by <a href="http://www.epsg.org/Geodetic.html">EPSG
     * </a> in <a href=
     * "http://www.epsg-registry.org/report.htm?type=selection&entity=urn:ogc:def:crs:EPSG::4326&reportDetail=long&title=WGS%2084&style=urn:uuid:report-style:default-with-code&style_name=OGP%20Default%20With%20Code"
     * >4326</a>. The record with an {@code srs_id} of -1 SHALL be used for
     * undefined Cartesian coordinate reference systems. The record with an
     * {@code srs_id} of 0 SHALL be used for undefined geographic coordinate
     * reference systems.
     * </blockquote>
     *
     * @throws SQLException   throws if SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 11",
            text = "The gpkg_spatial_ref_sys table SHALL contain at a minimum the records listed in Spatial Ref Sys Table Records. The record with an srs_id of 4326 SHALL correspond to WGS-84 as defined by EPSG in 4326. The record with an srs_id of -1 SHALL be used for undefined Cartesian coordinate reference systems. The record with an srs_id of 0 SHALL be used for undefined geographic coordinate reference systems.")
    public void requirement11() throws SQLException, AssertionError {
        if (this.hasSpatialReferenceSystemTable) {
            final String wgs1984Query = String.format("SELECT COUNT(*)\n" +
                            "FROM %s\n" +
                            "WHERE srs_id                   = 4326 AND\n" +
                            "      organization_coordsys_id = 4326 AND\n" +
                            "      (organization = 'EPSG' OR\n" +
                            "       organization = 'epsg');",
                    GeoPackageCore.SpatialRefSysTableName);

            final Integer wgs1984Record = JdbcUtility.selectOne(this.getSqliteConnection(),
                    wgs1984Query,
                    null,
                    resultSet -> resultSet.getInt(1));

            assertTrue(String.format("The %s table shall contain a record with srs_id of 4326, organization of \"EPSG\" or \"epsg\" and organization_coordsys_id of 4326",
                    GeoPackageCore.SpatialRefSysTableName),
                    wgs1984Record != null && wgs1984Record == 1,
                    Severity.Warning);

            final String undefinedCartesianQuery = String.format("SELECT COUNT(*)\n" +
                            "FROM %s\n" +
                            "WHERE srs_id                   = -1     AND\n" +
                            "      organization             = 'NONE' AND\n" +
                            "      organization_coordsys_id = -1     AND\n" +
                            "      definition               = 'undefined';",
                    GeoPackageCore.SpatialRefSysTableName);

            final Integer undefinedCartesianRecord = JdbcUtility.selectOne(this.getSqliteConnection(),
                    undefinedCartesianQuery,
                    null,
                    resultSet -> resultSet.getInt(1));

            assertTrue(String.format("The %s table shall contain a record with an srs_id of -1, an organization of \"NONE\", an organization_coordsys_id of -1, and definition \"undefined\" for undefined Cartesian coordinate reference systems",
                    GeoPackageCore.SpatialRefSysTableName),
                    undefinedCartesianRecord != null && undefinedCartesianRecord == 1,
                    Severity.Warning);

            final String undefinedGeographicQuery = String.format("SELECT COUNT(*)\n" +
                            "FROM %s\n" +
                            "WHERE srs_id                   = 0      AND\n" +
                            "      organization             = 'NONE' AND\n" +
                            "      organization_coordsys_id = 0      AND\n" +
                            "      definition               = 'undefined';",
                    GeoPackageCore.SpatialRefSysTableName);

            final Integer undefinedGeographicRecord = JdbcUtility.selectOne(this.getSqliteConnection(),
                    undefinedGeographicQuery,
                    null,
                    resultSet -> resultSet.getInt(1));

            assertTrue(String.format("The %s table shall contain a record with an srs_id of 0, an organization of \"NONE\", an organization_coordsys_id of 0, and definition \"undefined\" for undefined geographic coordinate reference systems.",
                    GeoPackageCore.SpatialRefSysTableName),
                    undefinedGeographicRecord != null && undefinedGeographicRecord == 1,
                    Severity.Warning);
        }
    }

    /**
     * Requirement 12
     * <p>
     * <blockquote>
     * The {@code gpkg_spatial_ref_sys} table in a GeoPackage SHALL contain
     * records to define all spatial reference systems used by features and
     * tiles in a GeoPackage.
     * </blockquote>
     *
     * @throws SQLException   throws if SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 12",
            text = "The gpkg_spatial_ref_sys table in a GeoPackage SHALL contain records to define all spatial reference systems used by features and tiles in a GeoPackage.")
    public void requirement12() throws SQLException, AssertionError {
        if (this.hasContentsTable && this.hasSpatialReferenceSystemTable) {
            final String query = String.format("SELECT DISTINCT srs_id as srsContents " +
                            "FROM            %s " +
                            "WHERE           srsContents " +
                            "NOT IN (SELECT srs_id FROM %s);",
                    GeoPackageCore.ContentsTableName,
                    GeoPackageCore.SpatialRefSysTableName);


            final List<Integer> invalidSrsIds = JdbcUtility.select(this.getSqliteConnection(),
                    query,
                    null,
                    resultSet -> resultSet.getInt("srsContents"));


            assertTrue(String.format("Not all srs_id's being used in a GeoPackage are defined. The following srs_id(s) are not in %s: %s",
                    GeoPackageCore.SpatialRefSysTableName,
                    invalidSrsIds.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", "))),
                    invalidSrsIds.isEmpty(),
                    Severity.Error);
        }
    }

    /**
     * Requirement 13
     * <p>
     * <blockquote>
     * A GeoPackage file SHALL include a {@code gpkg_contents} table per table
     * <a href="http://www.geopackage.org/spec/#gpkg_contents_cols">Contents
     * Table Definition</a> and <a href=
     * "http://www.geopackage.org/spec/#gpkg_contents_sql">gpkg_contents Table
     * Definition SQL</a>.
     * </blockquote>
     *
     * @throws SQLException   throws if SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 13",
            text = "A GeoPackage file SHALL include a gpkg_contents table per table Contents Table Definition and gpkg_contents Table Definition SQL.")
    public void requirement13() throws SQLException, AssertionError {
        if (this.hasContentsTable) {
            this.verifyTable(CoreVerifier.ContentTableDefinition);
        } else {
            throw new AssertionError(String.format("The GeoPackage does not contain a %s table. This is a required table for all GeoPackages.",
                    GeoPackageCore.ContentsTableName),
                    Severity.Error);
        }
    }

    /**
     * 1.1.3.1.1. Table Definition, Table 4. Contents Table Definition, srs_id
     * <p>
     * <blockquote>
     * Spatial Reference System ID: {@code gpkg_spatial_ref_sys.srs_id}; when
     * {@code data_type} is features, SHALL also match {@code
     * gpkg_geometry_columns.srs_id}; When {@code data_type} is tiles, SHALL
     * also match {@code gpkg_tile_matrix_set.srs_id}
     * </blockquote>
     *
     * @throws SQLException   throws if SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "1.1.3.1.1. Table Definition, Table 4. Contents Table Definition, srs_id",
            text = "Spatial Reference System ID: gpkg_spatial_ref_sys.srs_id; when data_type is features, SHALL also match gpkg_geometry_columns.srs_id; When data_type is tiles, SHALL also match gpkg_tile_matrix_set.srs_id")
    public void contentsTableDefinitionSrsId() throws SQLException, AssertionError {
        if (this.hasContentsTable) {
            final String featuresQuery = "SELECT contents.table_name,\n" +
                    "       contents.srs_id\n" +
                    "FROM gpkg_contents AS contents,\n" +
                    "     gpkg_geometry_columns as geometry\n" +
                    "where contents.data_type   = 'features'          COLLATE NOCASE AND\n" +
                    "      contents.table_name  = geometry.table_name COLLATE NOCASE AND\n" +
                    "      contents.srs_id     != geometry.srs_id;";

            final List<String> featureTableNames = JdbcUtility.select(this.getSqliteConnection(),
                    featuresQuery,
                    null,
                    resultSet -> resultSet.getString(1));

            final String tilesQuery = "SELECT contents.table_name,\n" +
                    "       contents.srs_id\n" +
                    "FROM gpkg_contents AS contents,\n" +
                    "     gpkg_tile_matrix_set as tiles\n" +
                    "where contents.data_type   = 'tiles'          COLLATE NOCASE AND\n" +
                    "      contents.table_name  = tiles.table_name COLLATE NOCASE AND\n" +
                    "      contents.srs_id     != tiles.srs_id;";

            final List<String> tilesTableNames = JdbcUtility.select(this.getSqliteConnection(),
                    tilesQuery,
                    null,
                    resultSet -> resultSet.getString(1));

            final StringBuilder error = new StringBuilder();

            if (!featureTableNames.isEmpty()) {
                error.append(String.format("The following %s entries with the following table_name(s) and data_type 'features' don't have an srs_id that matches their corresponding entry in %s:\n%s",
                        GeoPackageCore.ContentsTableName,
                        GeoPackageFeatures.GeometryColumnsTableName,
                        String.join(", ", featureTableNames)));
            }

            if (!tilesTableNames.isEmpty()) {
                if (error.length() > 0) {
                    error.append('\n');
                }

                error.append(String.format("The following %s entries with the following table_name(s) and data_type 'tiles' don't have an srs_id that matches their corresponding entry in %s:\n%s",
                        GeoPackageCore.ContentsTableName,
                        GeoPackageTiles.MatrixSetTableName,
                        String.join(", ", tilesTableNames)));
            }

            assertTrue(error.toString(),
                    error.length() == 0,
                    Severity.Warning);
        }
    }

    /**
     * Requirement 14
     * <p>
     * <blockquote>
     * The {@code table_name} column value in a {@code gpkg_contents} table row
     * SHALL contain the name of a SQLite table or view.
     * </blockquote>
     *
     * @throws SQLException   throws if an SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 14",
            text = "The table_name column value in a gpkg_contents table row SHALL contain the name of a SQLite table or view.")
    public void requirement14() throws SQLException, AssertionError {
        if (this.hasContentsTable) {
            final String query = String.format("SELECT DISTINCT table_name\n" +
                            "FROM %s\n" +
                            "WHERE table_name COLLATE NOCASE NOT IN\n" +
                            "      (SELECT name\n" +
                            "       FROM sqlite_master\n" +
                            "       WHERE (type = 'table' OR\n" +
                            "              type = 'view') AND\n" +
                            "             name = table_name COLLATE NOCASE);",
                    GeoPackageCore.ContentsTableName);

            final List<String> invalidContentsTableNames = JdbcUtility.select(this.getSqliteConnection(),
                    query,
                    null,
                    result -> result.getString(1));

            assertTrue(String.format("The following table_name(s) in %s are invalid: \n%s",
                    GeoPackageCore.ContentsTableName,
                    invalidContentsTableNames.stream()
                            .map(table_name -> String.format("\t%s", table_name))
                            .collect(Collectors.joining("\n"))),
                    invalidContentsTableNames.isEmpty(),
                    Severity.Warning);
        }
    }

    /**
     * Requirement 15
     * <p>
     * <blockquote>
     * Values of the {@code gpkg_contents} table {@code last_change}
     * column SHALL be in <a
     * href="http://www.iso.org/iso/catalogue_detail?csnumber=40874">ISO 8601
     * </a> format containing a complete date plus UTC hours, minutes, seconds
     * and a decimal fraction of a second, with a 'Z' ('zulu') suffix
     * indicating UTC. The ISO8601 format is as defined by the strftime function
     * %Y-%m-%dT%H:%M:%fZ format string applied to the current time.
     * </blockquote>
     *
     * @throws SQLException   throws if an SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 15",
            text = "Values of the gpkg_contents table last_change column SHALL be in ISO 8601 format containing a complete date plus UTC hours, minutes, seconds and a decimal fraction of a second, with a 'Z' ('zulu') suffix indicating UTC. The ISO8601 format is as defined by the strftime function %Y-%m-%dT%H:%M:%fZ format string applied to the current time.")
    public void requirement15() throws SQLException, AssertionError {
        if (this.hasContentsTable) {
            final String query = String.format("SELECT table_name, last_change FROM %s;", GeoPackageCore.ContentsTableName);

            final List<String> contentTableNamesWithBadDates = JdbcUtility.filterSelect(this.getSqliteConnection(),
                    query,
                    null,
                    resultSet -> {
                        final String lastChange = resultSet.getString(2);

                        try {
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SS'Z'").parse(lastChange);

                            return false;
                        } catch (final ParseException ignored) {
                            try {
                                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(lastChange);

                                return false;
                            } catch (final ParseException ignored2) {
                                return true;
                            }
                        }
                    },
                    resultSet -> resultSet.getString(1));


            assertTrue(String.format("The following table names in the %s table have a last_change value in an incorrect format: %s",
                    GeoPackageCore.ContentsTableName,
                    String.join(", ", contentTableNamesWithBadDates)),
                    contentTableNamesWithBadDates.isEmpty(),
                    Severity.Warning);
        }
    }

    /**
     * Requirement 16
     * <p>
     * <blockquote>
     * Values of the {@code gpkg_contents} table {@code srs_id}
     * column SHALL reference values in the {@code gpkg_spatial_ref_sys}
     * table {@code srs_id} column.
     * </blockquote>
     *
     * @throws SQLException   throws if an SQLException occurs
     * @throws AssertionError throws if it fails to meet the specified requirement
     */
    @Requirement(reference = "Requirement 16",
            text = "Values of the gpkg_contents table srs_id column SHALL reference values in the gpkg_spatial_ref_sys table srs_id column.")
    public void requirement16() throws SQLException, AssertionError {
        if (this.hasContentsTable) {
            final String query = String.format("SELECT table_name, srs_id\n" +
                            "FROM %s\n" +
                            "WHERE srs_id NOT IN\n" +
                            "      (SELECT srs_id\n" +
                            "       FROM %s);",
                    GeoPackageCore.ContentsTableName,
                    GeoPackageCore.SpatialRefSysTableName);

            final List<Pair<String, Integer>> invalidSrsIdsContentsTableNames = JdbcUtility.select(this.getSqliteConnection(),
                    query,
                    null,
                    result -> Pair.of(result.getString(1),
                            result.getInt(2)));

            assertTrue(String.format("The following table_name(s) in %s have an srs_id that doesn't exist in %s:\n%s",
                    GeoPackageCore.ContentsTableName,
                    GeoPackageCore.SpatialRefSysTableName,
                    invalidSrsIdsContentsTableNames.stream()
                            .map(table_name -> String.format("\t%s", table_name))
                            .collect(Collectors.joining("\n"))),
                    invalidSrsIdsContentsTableNames.isEmpty(),
                    Severity.Warning);
        }
    }

    private boolean doesTableExist(final String tableName) throws SQLException {
        return DatabaseUtility.doesTableOrViewExists(this.getSqliteConnection(), tableName);
    }

    private final File file;

    private static final TableDefinition ContentTableDefinition;
    private static final TableDefinition SpatialReferenceSystemDefinition;

    static {
        final Map<String, ColumnDefinition> contentColumns = new HashMap<>();

        contentColumns.put("table_name", new ColumnDefinition("TEXT", true, true, true, null));
        contentColumns.put("data_type", new ColumnDefinition("TEXT", true, false, false, null));
        contentColumns.put("identifier", new ColumnDefinition("TEXT", false, false, true, null));
        contentColumns.put("description", new ColumnDefinition("TEXT", false, false, false, "''"));
        contentColumns.put("last_change", new ColumnDefinition("DATETIME", true, false, false, "strftime('%Y-%m-%dT%H:%M:%fZ', 'now')"));
        contentColumns.put("min_x", new ColumnDefinition("DOUBLE", false, false, false, null));
        contentColumns.put("min_y", new ColumnDefinition("DOUBLE", false, false, false, null));
        contentColumns.put("max_x", new ColumnDefinition("DOUBLE", false, false, false, null));
        contentColumns.put("max_y", new ColumnDefinition("DOUBLE", false, false, false, null));
        contentColumns.put("srs_id", new ColumnDefinition("INTEGER", false, false, false, null));

        ContentTableDefinition = new TableDefinition(GeoPackageCore.ContentsTableName,
                contentColumns,
                new HashSet<>(Arrays.asList(new ForeignKeyDefinition(GeoPackageCore.SpatialRefSysTableName, "srs_id", "srs_id"))));

        final Map<String, ColumnDefinition> spatialReferenceSystemColumns = new HashMap<>();

        spatialReferenceSystemColumns.put("srs_name", new ColumnDefinition("TEXT", true, false, false, null));
        spatialReferenceSystemColumns.put("srs_id", new ColumnDefinition("INTEGER", true, true, true, null));
        spatialReferenceSystemColumns.put("organization", new ColumnDefinition("TEXT", true, false, false, null));
        spatialReferenceSystemColumns.put("organization_coordsys_id", new ColumnDefinition("INTEGER", true, false, false, null));
        spatialReferenceSystemColumns.put("definition", new ColumnDefinition("TEXT", true, false, false, null));
        spatialReferenceSystemColumns.put("description", new ColumnDefinition("TEXT", false, false, false, null));

        SpatialReferenceSystemDefinition = new TableDefinition(GeoPackageCore.SpatialRefSysTableName,
                spatialReferenceSystemColumns);
    }
}
