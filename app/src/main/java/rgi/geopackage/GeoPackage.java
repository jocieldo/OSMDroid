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
package rgi.geopackage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import rgi.geopackage.core.GeoPackageCore;
import rgi.geopackage.extensions.GeoPackageExtensions;
import rgi.geopackage.features.GeoPackageFeatures;
import rgi.geopackage.metadata.GeoPackageMetadata;
import rgi.geopackage.schema.GeoPackageSchema;
import rgi.geopackage.tiles.GeoPackageTiles;
import rgi.geopackage.utility.DatabaseUtility;
import rgi.geopackage.utility.DatabaseVersion;
import rgi.geopackage.utility.JournalMode;
import rgi.geopackage.utility.SynchronousState;
import rgi.geopackage.utility.ToggleState;
import rgi.geopackage.verification.ConformanceException;
import rgi.geopackage.verification.Severity;
import rgi.geopackage.verification.VerificationIssue;
import rgi.geopackage.verification.VerificationLevel;

/**
 * Implementation of the <a href="http://www.geopackage.org/spec/">OGC GeoPackage specification</a>
 *
 * @author Luke Lambert
 */
public class GeoPackage implements AutoCloseable {
    /**
     * @param file Location on disk that represents where an existing GeoPackage will opened and/or created
     * @throws ClassNotFoundException when the SQLite JDBC driver cannot be found
     * @throws ConformanceException   when the verifyConformance parameter is true, and if
     *                                there are any conformance violations with the severity
     *                                {@link Severity#Error}
     * @throws IOException            when openMode is set to OpenMode.Create, and the file already
     *                                exists, openMode is set to OpenMode.Open, and the file does not
     *                                exist, or if there is a file read error
     * @throws SQLException           in various cases where interaction with the JDBC connection fails
     */
    public GeoPackage(final File file) throws ClassNotFoundException, ConformanceException, IOException, SQLException {
        this(file, VerificationLevel.Fast, GeoPackage.OpenMode.OpenOrCreate);
    }

    /**
     * @param file              Location on disk that represents where an existing GeoPackage will opened and/or created
     * @param verificationLevel Controls the level of verification testing performed on this
     *                          GeoPackage.  If verificationLevel is not None
     *                          {@link #verify()} is called automatically and will throw if
     *                          there are any conformance violations with the severity
     *                          {@link Severity#Error}.  Throwing from this method means
     *                          that it won't be possible to instantiate a GeoPackage object
     *                          based on an SQLite "GeoPackage" file with severe errors.
     * @throws ClassNotFoundException When the SQLite JDBC driver cannot be found
     * @throws ConformanceException   When the verifyConformance parameter is true, and if
     *                                there are any conformance violations with the severity
     *                                {@link Severity#Error}
     * @throws IOException            When openMode is set to OpenMode.Create, and the file
     *                                already exists, openMode is set to OpenMode.Open, and the
     *                                file does not exist, or if there is a file read error
     * @throws SQLException           In various cases where interaction with the JDBC connection fails
     */
    public GeoPackage(final File file, final VerificationLevel verificationLevel) throws ClassNotFoundException, ConformanceException, IOException, SQLException {
        this(file, verificationLevel, GeoPackage.OpenMode.OpenOrCreate);
    }

    /**
     * @param file     Location on disk that represents where an existing GeoPackage will opened and/or created
     * @param openMode Controls the file creation/opening behavior
     * @throws ClassNotFoundException when the SQLite JDBC driver cannot be found
     * @throws ConformanceException   when the verifyConformance parameter is true, and if
     *                                there are any conformance violations with the severity
     *                                {@link Severity#Error}
     * @throws IOException            when openMode is set to OpenMode.Create, and the file already
     *                                exists, openMode is set to OpenMode.Open, and the file does not
     *                                exist, or if there is a file read error
     * @throws SQLException           in various cases where interaction with the JDBC connection fails
     */
    public GeoPackage(final File file, final GeoPackage.OpenMode openMode) throws ClassNotFoundException, ConformanceException, IOException, SQLException {
        this(file, VerificationLevel.Fast, openMode);
    }

    /**
     * @param file              Location on disk that represents where an existing GeoPackage
     *                          will opened and/or created
     * @param verificationLevel Indicates whether {@link #verify()} should be called
     *                          automatically. If verifyConformance is true and
     *                          {@link #verify()} is called automatically, it will throw if
     *                          there are any conformance violations with the severity
     *                          {@link Severity#Error}. Throwing from this method means that
     *                          it won't be possible to instantiate a GeoPackage object based
     *                          on an SQLite "GeoPackage" file with severe errors.
     * @param openMode          Controls the file creation/opening behavior
     * @throws ClassNotFoundException     when the SQLite JDBC driver cannot be found
     * @throws ConformanceException       when the verifyConformance parameter is true, and if there
     *                                    are any conformance violations with the severity
     *                                    {@link Severity#Error}
     * @throws IOException                when openMode is set to OpenMode.Create, and the file already
     *                                    exists, openMode is set to OpenMode.Open, and the file does
     *                                    not exist, or if there is a file read error
     * @throws FileAlreadyExistsException when openMode is set to OpenMode.Create, and the file already
     *                                    exists
     * @throws FileNotFoundException      when openMode is set to OpenMode.Open, and the file does not
     *                                    exist
     * @throws SQLException               in various cases where interaction with the JDBC connection
     *                                    fails
     */
    public GeoPackage(final File file, final VerificationLevel verificationLevel, final GeoPackage.OpenMode openMode) throws ClassNotFoundException, ConformanceException, IOException, SQLException {
        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }

        if (verificationLevel == null) {
            throw new IllegalArgumentException("Verification level may not be null");
        }

        if (openMode == null) {
            throw new IllegalArgumentException("Open mode may not be null");
        }

        final boolean isNewFile = !file.exists();

        if (openMode == GeoPackage.OpenMode.Create && !isNewFile) {
            throw new FileAlreadyExistsException(file.getAbsolutePath());
        }

        if (openMode == GeoPackage.OpenMode.Open && isNewFile) {
            throw new FileNotFoundException(String.format("%s does not exist", file.getAbsolutePath()));
        }

        this.file = file;

        this.verificationLevel = verificationLevel;

        Class.forName("org.sqlite.JDBC");   // Register the driver

        this.databaseConnection = DriverManager.getConnection("jdbc:sqlite:" + file.toURI()); // Initialize the database connection

        try {
            DatabaseUtility.setPragmaSynchronous(this.databaseConnection, SynchronousState.OFF);
            DatabaseUtility.setPragmaForeignKeys(this.databaseConnection, ToggleState.ON);
            DatabaseUtility.setPragmaJournalMode(this.databaseConnection, JournalMode.MEMORY);

            // This was moved below the PRAGMAs because it starts a transaction and causes setPragmaSynchronous to throw an exception
            this.databaseConnection.setAutoCommit(false);

            this.core = new GeoPackageCore(this.databaseConnection, isNewFile);
            this.features = new GeoPackageFeatures(this.databaseConnection, this.core);
            this.tiles = new GeoPackageTiles(this.databaseConnection, this.core);
            this.schema = new GeoPackageSchema(this.databaseConnection);
            this.metadata = new GeoPackageMetadata(this.databaseConnection);
            this.extensions = new GeoPackageExtensions(this.databaseConnection, this.core);

            if (isNewFile) {
                DatabaseUtility.setApplicationId(this.databaseConnection,
                        ByteBuffer.wrap(GeoPackage.GeoPackageSqliteApplicationId)
                                .asIntBuffer()
                                .get());

                // The current version of GeoPackage is 1.2, which is what
                // will be used if this file is brand new. This value can't be
                // assumed if we're opening a previously created GeoPackage.

                // Requirement 2
                // ... A GeoPackage SHALL contain an appropriate value in
                // "user_version" field of the SQLite database header to
                // indicate its version. The value SHALL be in integer with a
                // major version, two-digit minor version, and two-digit
                // bug-fix. For GeoPackage Version 1.2 this value is 0x000027D8
                // (the hexadecimal value for 10200).
                final int userVersion = 10200;

                DatabaseUtility.setUserVersion(this.databaseConnection,
                        userVersion);

                this.databaseConnection.commit();
            }

            if (verificationLevel != VerificationLevel.None) {
                this.verify();
            }

            try {
                this.sqliteVersion = DatabaseUtility.getSqliteVersion(this.file);
            } catch (final Exception ex) {
                throw new IOException("Unable to read SQLite version: " + ex.getMessage());
            }
        } catch (final SQLException | ConformanceException | IOException ex) {
            this.close();

            // If anything goes wrong, clean up the new file that may have been created
            if (isNewFile && file.exists()) {
                file.delete();
            }

            throw ex;
        }
    }

    /**
     * The file associated with this GeoPackage
     *
     * @return the file
     */
    public File getFile() {
        return this.file;
    }

    /**
     * The version of the SQLite database associated with this GeoPackage
     *
     * @return the sqliteVersion
     */
    public DatabaseVersion getSqliteVersion() {
        return this.sqliteVersion;
    }

    /**
     * The application id of the SQLite database
     *
     * @return Returns the application id of the SQLite database.  For a GeoPackage, by its standard, this must be 0x47503130 ("GP10" in ASCII)
     * @throws SQLException Throws if there is an SQL error
     */
    public int getApplicationId() throws SQLException {
        return DatabaseUtility.getApplicationId(this.databaseConnection);
    }

    /**
     * Closes the connection to the underlying SQLite database file
     *
     * @throws SQLException throws if SQLException occurs
     */
    @Override
    public void close() throws SQLException {
        if (this.databaseConnection != null &&
                !this.databaseConnection.isClosed()) {
            this.databaseConnection.rollback(); // When Connection.close() is called, pending transactions are either automatically committed or rolled back depending on implementation defined behavior.  Make the call explicitly to avoid relying on implementation defined behavior.
            this.databaseConnection.close();
        }
    }

    /**
     * Requirements this GeoPackage failed to meet
     *
     * @return the GeoPackage requirements this GeoPackage fails to conform to
     * @throws SQLException throws if SQLException occurs
     */
    public Collection<VerificationIssue> getVerificationIssues() throws SQLException {
        return this.getVerificationIssues(false);
    }

    /**
     * Requirements this GeoPackage failed to meet
     *
     * @param continueAfterCoreErrors If true, GeoPackage subsystem requirement violations will be
     *                                reported even if there are fatal violations in the core.
     *                                Subsystem verifications assumes that a GeoPackage is at
     *                                least minimally operable (e.g. core tables are defined to
     *                                the standard), and may behave unexpectedly if the GeoPackage
     *                                does not. In this state, the reported failures of GeoPackage
     *                                subsystems may only be of minimal value.
     * @return the GeoPackage requirements this GeoPackage fails to conform to
     * @throws SQLException throws if SQLException occurs
     */
    public Collection<VerificationIssue> getVerificationIssues(final boolean continueAfterCoreErrors) throws SQLException {
        final Collection<VerificationIssue> verificationIssues = new ArrayList<>();

        verificationIssues.addAll(this.core.getVerificationIssues(this.file, this.verificationLevel));

        // Skip verifying GeoPackage subsystems if there are fatal errors in core
        if (continueAfterCoreErrors || !verificationIssues.stream().anyMatch(verificationIssue -> verificationIssue.getSeverity() == Severity.Error)) {
            verificationIssues.addAll(this.features.getVerificationIssues(this.verificationLevel));
            verificationIssues.addAll(this.tiles.getVerificationIssues(this.verificationLevel));
            verificationIssues.addAll(this.schema.getVerificationIssues(this.verificationLevel));
            verificationIssues.addAll(this.metadata.getVerificationIssues(this.verificationLevel));
            verificationIssues.addAll(this.extensions.getVerificationIssues(this.verificationLevel));
        }

        return verificationIssues;
    }

    /**
     * Verifies that the GeoPackage meets the core requirements of the GeoPackage specification
     *
     * @throws ConformanceException throws if the GeoPackage fails to meet the necessary requirements
     * @throws SQLException         throws if SQLException occurs
     */
    public void verify() throws ConformanceException, SQLException {
        //final long startTime = System.nanoTime();

        final Collection<VerificationIssue> verificationIssues = this.getVerificationIssues();

        //System.out.println(String.format("GeoPackage took %.2f seconds to verify.", (System.nanoTime() - startTime)/1.0e9));

        if (!verificationIssues.isEmpty()) {
            final ConformanceException conformanceException = new ConformanceException(verificationIssues);

            //noinspection ThrowablePrintedToSystemOut
            System.out.println(conformanceException);

            if (verificationIssues.stream().anyMatch(verificationIssue -> verificationIssue.getSeverity() == Severity.Error)) {
                throw conformanceException;
            }
        }
    }

    /**
     * Access to GeoPackage's "core" functionality
     *
     * @return returns a handle to a GeoPackageCore object
     */
    public GeoPackageCore core() {
        return this.core;
    }

    /**
     * Access to GeoPackage's "features" functionality
     *
     * @return returns a handle to a GeoPackageFeatures object
     */
    public GeoPackageFeatures features() {
        return this.features;
    }

    /**
     * Access to GeoPackage's "tiles" functionality
     *
     * @return returns a handle to a GeoPackageTiles object
     */
    public GeoPackageTiles tiles() {
        return this.tiles;
    }

    /**
     * Access to GeoPackage's "schema" functionality
     *
     * @return returns a handle to a GeoPackageTiles object
     */
    public GeoPackageSchema schema() {
        return this.schema;
    }

    /**
     * Access to GeoPackage's "extensions" functionality
     *
     * @return returns a handle to a GeoPackageExtensions object
     */
    public GeoPackageExtensions extensions() {
        return this.extensions;
    }

    /**
     * Access to GeoPackage's "metadata" functionality
     *
     * @return returns a handle to a GeoPackageMetadata object
     */
    public GeoPackageMetadata metadata() {
        return this.metadata;
    }

    /**
     * @author Luke Lambert
     */
    public enum OpenMode {
        /**
         * Open or Create a GeoPackage
         */
        OpenOrCreate,

        /**
         * Open an Existing GeoPackage
         */
        Open,

        // TODO: OpenReadOnly

        /**
         * Create a new GeoPackage
         */
        Create
    }

    private final File file;
    private final Connection databaseConnection;
    private final DatabaseVersion sqliteVersion;
    private final VerificationLevel verificationLevel;
    private final GeoPackageCore core;
    private final GeoPackageFeatures features;
    private final GeoPackageTiles tiles;
    private final GeoPackageSchema schema;
    private final GeoPackageMetadata metadata;
    private final GeoPackageExtensions extensions;

    private static final byte[] GeoPackageSqliteApplicationId = {(byte) 'G', (byte) 'P', (byte) 'K', (byte) 'G'};
}
