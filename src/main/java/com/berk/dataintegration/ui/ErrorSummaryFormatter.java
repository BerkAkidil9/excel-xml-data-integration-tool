package com.berk.dataintegration.ui;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ErrorSummaryFormatter {
    private static final List<String> DISPLAY_ORDER = List.of(
            "Contract",
            "Field",
            "XML schema",
            "Parse",
            "Reference",
            "Business rule",
            "I/O"
    );

    private ErrorSummaryFormatter() {
    }

    static String format(List<ValidationError> errors) {
        List<ValidationError> safeErrors = errors == null ? List.of() : errors;
        if (safeErrors.isEmpty()) {
            return "No errors.";
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String label : DISPLAY_ORDER) {
            counts.put(label, 0);
        }
        for (ValidationError error : safeErrors) {
            counts.merge(label(error), 1, Integer::sum);
        }

        StringBuilder builder = new StringBuilder("Errors: ");
        builder.append(safeErrors.size());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 0) {
                builder.append("   ");
                builder.append(entry.getKey());
                builder.append(": ");
                builder.append(entry.getValue());
            }
        }
        return builder.toString();
    }

    private static String label(ValidationError error) {
        if (error.code() == ValidationCode.XML_SCHEMA_VALIDATION_FAILED) {
            return "XML schema";
        }
        ValidationCategory category = error.category();
        if (category == null) {
            return "Other";
        }
        return switch (category) {
            case CONTRACT -> "Contract";
            case FIELD -> "Field";
            case PARSE -> "Parse";
            case REFERENCE -> "Reference";
            case BUSINESS_RULE -> "Business rule";
            case IO -> "I/O";
        };
    }
}
