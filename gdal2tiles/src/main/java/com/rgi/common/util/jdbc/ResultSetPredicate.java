package com.rgi.common.util.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Luke Lambert
 */
@FunctionalInterface
public interface ResultSetPredicate {
    /**
     * Returns true if the given {@link ResultSet} satisfies the predicate, false otherwise
     *
     * @param resultSet The result set containing the elements
     * @return Returns true if the given {@link ResultSet} satisfies the predicate, false otherwise
     * @throws SQLException Throws if an SQLException occurs
     */
    boolean apply(final ResultSet resultSet) throws SQLException;
}
