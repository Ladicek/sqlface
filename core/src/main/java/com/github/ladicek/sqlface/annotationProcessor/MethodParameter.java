package com.github.ladicek.sqlface.annotationProcessor;

import javax.lang.model.type.TypeMirror;

final class MethodParameter {
    private final TypeMirror type;
    private final String name;

    MethodParameter(TypeMirror type, String name) {
        this.type = type;
        this.name = name;
    }

    public String type() {
        return type.toString();
    }

    public String name() {
        return name;
    }
}
