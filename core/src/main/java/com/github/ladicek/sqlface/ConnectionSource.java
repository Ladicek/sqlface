package com.github.ladicek.sqlface;

import java.sql.Connection;

public interface ConnectionSource {
    Connection get();
}
