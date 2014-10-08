package com.github.ladicek.sqlface.annotationProcessor;

import java.util.List;

final class GeneratedInterface {
    private final String pkg;
    private final String name;
    private final List<GeneratedMethod> methods;

    GeneratedInterface(String pkg, String name, List<GeneratedMethod> methods) {
        this.pkg = pkg;
        this.name = name;
        this.methods = methods;
    }

    public String pkg() {
        return pkg;
    }

    public String name() {
        return name;
    }

    public List<GeneratedMethod> methods() {
        return methods;
    }
}
