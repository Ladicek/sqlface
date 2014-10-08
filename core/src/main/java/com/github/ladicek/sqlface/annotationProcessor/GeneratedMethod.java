package com.github.ladicek.sqlface.annotationProcessor;

import java.util.List;

abstract class GeneratedMethod {
    private final String name;
    private final List<MethodParameter> parameters;
    private final String sql;

    protected GeneratedMethod(String name, List<MethodParameter> parameters, String sql) {
        this.parameters = parameters;
        this.name = name;
        this.sql = sql;
    }

    public final String name() {
        return name;
    }

    public final List<MethodParameter> parameters() {
        return parameters;
    }

    public final String sql() {
        return sql;
    }

    public final boolean isQuery() {
        return this instanceof QueryMethod;
    }

    public final boolean isUpdate() {
        return this instanceof UpdateMethod;
    }
}
