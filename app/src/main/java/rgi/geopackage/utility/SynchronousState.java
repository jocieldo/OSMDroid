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
 * <a href="https://www.sqlite.org/pragma.html#pragma_synchronous">SQLite
 * synchronous PRAGMA reference</a>
 *
 * @author Luke Lambert
 */
public enum SynchronousState {
    /**
     * With synchronous OFF (0), SQLite continues without syncing as soon as it
     * has handed data off to the operating system. If the application running
     * SQLite crashes, the data will be safe, but the database might become
     * corrupted if the operating system crashes or the computer loses power
     * before that data has been written to the disk surface. On the other hand,
     * commits can be orders of magnitude faster with synchronous OFF.
     */
    OFF(0),

    /**
     * When synchronous is NORMAL (1), the SQLite database engine will still
     * sync at the most critical moments, but less often than in FULL mode.
     * There is a very small (though non-zero) chance that a power failure at
     * just the wrong time could corrupt the database in NORMAL mode. But in
     * practice, you are more likely to suffer a catastrophic disk failure or
     * some other unrecoverable hardware fault. Many applications choose NORMAL
     * when in WAL mode.
     */
    NORMAL(1),

    /**
     * When synchronous is FULL (2), the SQLite database engine will use the
     * xSync method of the VFS to ensure that all content is safely written to
     * the disk surface prior to continuing. This ensures that an operating
     * system crash or power failure will not corrupt the database. FULL
     * synchronous is very safe, but it is also slower. FULL is the most
     * commonly used synchronous setting when not in WAL mode.
     */
    FULL(2),

    /**
     * EXTRA synchronous is like FULL with the addition that the directory
     * containing a rollback journal is synced after that journal is unlinked to
     * commit a transaction in DELETE mode. EXTRA provides additional durability
     * if the commit is followed closely by a power loss.
     */
    EXTRA(3);

    SynchronousState(final int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }

    private final int state;
}
