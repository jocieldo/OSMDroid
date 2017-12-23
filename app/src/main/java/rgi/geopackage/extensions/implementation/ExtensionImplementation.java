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

package rgi.geopackage.extensions.implementation;

import java.sql.Connection;
import java.sql.SQLException;

import rgi.geopackage.GeoPackage;
import rgi.geopackage.core.GeoPackageCore;
import rgi.geopackage.extensions.Extension;
import rgi.geopackage.extensions.GeoPackageExtensions;
import rgi.geopackage.extensions.Scope;

/**
 * Abstract class extended to represent the operations of a {@link GeoPackage}
 * extension
 *
 * @author Luke Lambert
 */
public abstract class ExtensionImplementation {
    /**
     * @param databaseConnection   The open connection to the database that contains a GeoPackage
     * @param geoPackageCore       'Core' subsystem of the {@link GeoPackage} implementation
     * @param geoPackageExtensions 'Extensions' subsystem of the {@link GeoPackage} implementation
     * @throws SQLException if getting the corresponding {@link Extension} from the
     *                      {@link GeoPackage} fails
     */
    public ExtensionImplementation(final Connection databaseConnection,
                                   final GeoPackageCore geoPackageCore,
                                   final GeoPackageExtensions geoPackageExtensions) throws SQLException {
        if (databaseConnection == null || databaseConnection.isClosed()) {
            throw new IllegalArgumentException("Database connection may not be null or closed");
        }

        if (geoPackageCore == null) {
            throw new IllegalArgumentException("GeoPackage core subsystem may not be null");
        }

        if (geoPackageExtensions == null) {
            throw new IllegalArgumentException("GeoPackage extensions subsystem may not be null");
        }

        this.databaseConnection = databaseConnection;
        this.geoPackageCore = geoPackageCore;
        this.geoPackageExtensions = geoPackageExtensions;

        this.extension = this.geoPackageExtensions.getExtension(this.getTableName(),
                this.getColumnName(),
                this.getExtensionName());
    }

    /**
     * @return Name of the table that requires the extension. When NULL, the
     * extension is required for the entire GeoPackage. SHALL NOT
     * be NULL when the column_name is not NULL.
     */
    public abstract String getTableName();

    /**
     * @return Name of the column that requires the extension. When NULL, the
     * extension is required for the entire table.
     */
    public abstract String getColumnName();

    /**
     * @return The case sensitive name of the extension that is required, in
     * the form <author>_<extension_name>.
     */
    public abstract String getExtensionName();

    /**
     * @return Definition of the extension in the form specified by the
     * template in <a href=
     * "http://www.geopackage.org/spec/#extension_template"
     * GeoPackage Extension Template (Normative)</a> or reference
     * thereto.
     */
    public abstract String getDefinition();

    /**
     * @return Indicates scope of extension effects on readers / writers:
     * read-write or write-only in lowercase.
     */
    public abstract Scope getScope();

    protected void addExtensionEntry() throws SQLException {
        if (this.extension == null) {
            this.extension = this.geoPackageExtensions.addExtension(this.getTableName(),
                    this.getColumnName(),
                    this.getExtensionName(),
                    this.getDefinition(),
                    this.getScope());
        }
    }

    protected final Connection databaseConnection;
    protected final GeoPackageCore geoPackageCore;
    protected final GeoPackageExtensions geoPackageExtensions;

    private Extension extension;
}
