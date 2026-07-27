package com.berk.dataintegration.xml;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.xml.sax.SAXParseException;

final class XmlValidationErrors {
    private XmlValidationErrors() {
    }

    static ValidationError xmlError(
            ValidationCode code,
            ValidationCategory category,
            String message,
            String field,
            String recordId,
            String sourceFile,
            String xmlPath,
            Integer line,
            Integer column,
            String value
    ) {
        return new ValidationError(
                code,
                category,
                message,
                field,
                recordId,
                sourceFile,
                xmlPath,
                line,
                column == null ? null : column.toString(),
                value
        );
    }

    static ValidationError fromParseException(
            ValidationCode code,
            ValidationCategory category,
            String message,
            String sourceFile,
            SAXParseException exception
    ) {
        return xmlError(
                code,
                category,
                message,
                null,
                null,
                sourceFile,
                null,
                positiveOrNull(exception.getLineNumber()),
                positiveOrNull(exception.getColumnNumber()),
                safeMessage(exception)
        );
    }

    static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message;
    }

    private static Integer positiveOrNull(int value) {
        return value > 0 ? value : null;
    }
}
