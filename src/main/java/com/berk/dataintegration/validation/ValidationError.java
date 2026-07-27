package com.berk.dataintegration.validation;

public record ValidationError(
        ValidationCode code,
        ValidationCategory category,
        String message,
        String field,
        String recordId,
        String sourceFile,
        String sheet,
        Integer row,
        String column,
        String value
) {
    public static ValidationError of(
            ValidationCode code,
            ValidationCategory category,
            String message,
            String field,
            String recordId
    ) {
        return new ValidationError(code, category, message, field, recordId, null, null, null, null, null);
    }
}
