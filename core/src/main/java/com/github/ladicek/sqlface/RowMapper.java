package com.github.ladicek.sqlface;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface RowMapper<T> {
    T mapRow(ResultSet row, int rowNumber) throws SQLException;
}
