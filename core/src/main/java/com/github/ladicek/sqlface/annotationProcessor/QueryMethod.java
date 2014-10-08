package com.github.ladicek.sqlface.annotationProcessor;

import javax.lang.model.type.TypeMirror;
import java.util.List;

final class QueryMethod extends GeneratedMethod {
    private final TypeMirror returnType;
    private final Kind kind; // TODO is this needed?

    QueryMethod(String name, List<MethodParameter> parameters, String sql, TypeMirror returnType, Kind kind) {
        super(name, parameters, sql);
        this.returnType = returnType;
        this.kind = kind;
    }

    public String returnType() {
        return returnType.toString();
    }

    static enum Kind {
        SINGLE_RESULT,
        MULTI_RESULT
    }
}
