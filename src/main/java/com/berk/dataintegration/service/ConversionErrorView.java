package com.berk.dataintegration.service;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;

public record ConversionErrorView(
        ValidationCode code,
        ValidationCategory category,
        ConversionErrorPhase phase,
        ConversionErrorType type,
        String message,
        String sourceFile,
        String sheetOrPath,
        Integer row,
        String column,
        String field,
        String recordId,
        String value
) {
    public static ConversionErrorView from(ValidationError error) {
        return new ConversionErrorView(
                error.code(),
                error.category(),
                phase(error),
                ConversionErrorType.from(error),
                error.message(),
                error.sourceFile(),
                error.sheet(),
                error.row(),
                error.column(),
                error.field(),
                error.recordId(),
                error.value()
        );
    }

    public String location() {
        StringBuilder builder = new StringBuilder();
        append(builder, sourceFile);
        append(builder, sheetOrPath);
        if (row != null) {
            append(builder, "row " + row);
        }
        if (column != null) {
            append(builder, "column " + column);
        }
        return builder.toString();
    }

    private static ConversionErrorPhase phase(ValidationError error) {
        if (error.code() == ValidationCode.CONVERSION_OUTPUT_FAILED) {
            return ConversionErrorPhase.OUTPUT;
        }
        if (error.code() == ValidationCode.CONVERSION_INPUT_FAILED
                || error.category() == ValidationCategory.PARSE) {
            return ConversionErrorPhase.INPUT;
        }
        return ConversionErrorPhase.VALIDATION;
    }

    private static void append(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(" / ");
        }
        builder.append(value);
    }
}
