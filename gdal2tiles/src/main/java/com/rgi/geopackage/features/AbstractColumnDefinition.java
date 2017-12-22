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

package com.rgi.geopackage.features;

import com.rgi.geopackage.utility.DatabaseUtility;

import java.util.EnumSet;

/**
 * Object to capture a column's definition: name, type, and constraints
 * <br>
 * <br>
 * Supported constraints:
 * <br>
 * <ul>
 * <li>Flags (primary key, auto increment, not null, unique</li>
 * <li>Check</li>
 * <li>Default</li>
 * </ul>
 * Currently unsupported constraints:
 * <br>
 * <ul>
 * <li>Primary key asc/desc</li>
 * <li>Conflict clauses</li>
 * <li>Collate</li>
 * <li>Foreign Key</li>
 * </ul>
 * For a picture of SQLite's full range of supported constraints see: <a href=
 * "https://www.sqlite.org/lang_createtable.html">SQLite's create table
 * documentation</a>
 *
 * @author Luke Lambert
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class AbstractColumnDefinition {
    /**
     * Constructor
     *
     * @param name            Column name
     * @param type            Column type
     * @param checkExpression SQLite "check" expression. Ignored if null.  Each time a new
     *                        row is inserted into the table or an existing row is
     *                        updated, the expression associated with each CHECK
     *                        constraint is evaluated and cast to a NUMERIC value in the
     *                        same way as a CAST expression. If the result is zero
     *                        (integer value 0 or real value 0.0), then a constraint
     *                        violation has occurred. If the CHECK expression evaluates
     *                        to NULL, or any other non-zero value, it is not a constraint
     *                        violation. The expression of a CHECK constraint may not
     *                        contain a subquery.
     * @param flags           Column constraint flags
     * @param defaultValue    Column default value
     * @param comment         Comment to be added to the table definition. Ignored if
     *                        null.
     */
    protected AbstractColumnDefinition(final String name,
                                       final String type,
                                       final EnumSet<ColumnFlag> flags,
                                       final String checkExpression,
                                       final ColumnDefault defaultValue,
                                       final String comment) {
        DatabaseUtility.validateColumnName(name);

        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("The column type may not be null or empty");
        }

        this.flags = flags == null ? EnumSet.noneOf(ColumnFlag.class)
                : flags;

        if (defaultValue == null) {
            throw new IllegalArgumentException("The default type may not be null. Use ColumnDefault.Null to specify a null value, or ColumnDefault.None to leave the default unspecified.");
        }

        if (this.flags.contains(ColumnFlag.NotNull) &&
                !this.flags.contains(ColumnFlag.PrimaryKey) &&   // Ignore primary keys. They're always auto-increment, and can not use a default value
                (defaultValue.equals(ColumnDefault.None) ||
                        defaultValue.equals(ColumnDefault.Null))) {
            throw new IllegalArgumentException("Column flags that specify that values may not be null must provide a default column value other than ColumnDefault.None or ColumnDefault.Null");
        }

        this.name = name;
        this.type = type;
        this.checkExpression = checkExpression;
        this.defaultValue = defaultValue;
        this.comment = comment;

        if (this.comment != null && this.comment.contains("\n")) {
            throw new IllegalArgumentException("The comment may not contain any line break characters");
        }
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    /**
     * Determines if a column definition includes a particular column
     * constraint flag
     *
     * @param columnFlag Flag to check the presence of
     * @return true if this column was defined with the particular column flag
     */
    public boolean hasFlag(final ColumnFlag columnFlag) {
        return this.flags.contains(columnFlag);
    }

    public String getCheckExpression() {
        return this.checkExpression;
    }

    public ColumnDefault getDefaultValue() {
        return this.defaultValue;
    }

    public String getComment() {
        return this.comment;
    }

    private final String name;
    private final String type;
    private final EnumSet<ColumnFlag> flags;
    private final String checkExpression;
    private final ColumnDefault defaultValue;
    private final String comment;

}
