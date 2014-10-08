package com.github.ladicek.sqlface.annotationProcessor;

import javax.lang.model.element.Element;

final class CantGenerateException extends Exception {
    final String message;
    final Element element;

    CantGenerateException(String message, Element element) {
        this.message = message;
        this.element = element;
    }
}
