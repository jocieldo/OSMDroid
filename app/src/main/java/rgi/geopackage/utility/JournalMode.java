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

/**
 * <a href="https://www.sqlite.org/pragma.html#pragma_journal_mode">SQLite
 * journal_mode PRAGMA reference</a>
 *
 * @author Luke Lambert
 */
public enum JournalMode {
    /**
     * The DELETE journaling mode is the normal behavior. In the DELETE mode,
     * the rollback journal is deleted at the conclusion of each transaction.
     * Indeed, the delete operation is the action that causes the transaction to
     * commit. (See the document titled Atomic Commit In SQLite for additional
     * detail.)
     */
    DELETE,

    /**
     * The TRUNCATE journaling mode commits transactions by truncating the
     * rollback journal to zero-length instead of deleting it. On many systems,
     * truncating a file is much faster than deleting the file since the
     * containing directory does not need to be changed.
     */
    TRUNCATE,

    /**
     * The PERSIST journaling mode prevents the rollback journal from being
     * deleted at the end of each transaction. Instead, the header of the
     * journal is overwritten with zeros. This will prevent other database
     * connections from rolling the journal back. The PERSIST journaling mode is
     * useful as an optimization on platforms where deleting or truncating a
     * file is much more expensive than overwriting the first block of a file
     * with zeros. See also: PRAGMA journal_size_limit and
     * SQLITE_DEFAULT_JOURNAL_SIZE_LIMIT.
     */
    PERSIST,

    /**
     * The MEMORY journaling mode stores the rollback journal in volatile RAM.
     * This saves disk I/O but at the expense of database safety and integrity.
     * If the application using SQLite crashes in the middle of a transaction
     * when the MEMORY journaling mode is set, then the database file will very
     * likely go corrupt.
     */
    MEMORY,

    /**
     * The WAL journaling mode uses a write-ahead log instead of a rollback
     * journal to implement transactions. The WAL journaling mode is persistent;
     * after being set it stays in effect across multiple database connections
     * and after closing and reopening the database. A database in WAL
     * journaling mode can only be accessed by SQLite version 3.7.0 (2010-07-21)
     * or later.
     */
    WAL,

    /**
     * The OFF journaling mode disables the rollback journal completely. No
     * rollback journal is ever created and hence there is never a rollback
     * journal to delete. The OFF journaling mode disables the atomic commit and
     * rollback capabilities of SQLite. The ROLLBACK command no longer works; it
     * behaves in an undefined way. Applications must avoid using the ROLLBACK
     * command when the journal mode is OFF. If the application crashes in the
     * middle of a transaction when the OFF journaling mode is set, then the
     * database file will very likely go corrupt.
     */
    OFF;
}
