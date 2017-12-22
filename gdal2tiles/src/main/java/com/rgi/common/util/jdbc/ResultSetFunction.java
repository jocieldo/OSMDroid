package com.rgi.common.util.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @param <T> the type of the input to the operation
 * @author Luke Lambert
 */
@FunctionalInterface
public interface ResultSetFunction<T> {
    /**
     * The function that performs an operation on the given {@link ResultSet}
     * and returns the type of the input to the operation after operation is applied
     *
     * @param resultSet the {@link ResultSet} to perform the operations on
     * @return the type of the input to the operation after applying the operation
     * @throws SQLException throws if an SQLException occurs
     */
    T apply(final ResultSet resultSet) throws SQLException;
}
