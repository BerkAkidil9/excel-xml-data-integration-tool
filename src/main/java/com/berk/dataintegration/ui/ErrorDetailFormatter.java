package com.berk.dataintegration.ui;

import com.berk.dataintegration.validation.ValidationError;

final class ErrorDetailFormatter {
    private ErrorDetailFormatter() {
    }

    static String format(ValidationError error) {
        if (error == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        append(builder, "Code", error.code());
        append(builder, "Category", error.category());
        append(builder, "Message", error.message());
        append(builder, "Field", error.field());
        append(builder, "Record", error.recordId());
        append(builder, "Source", error.sourceFile());
        append(builder, "Sheet/Path", error.sheet());
        append(builder, "Row", error.row());
        append(builder, "Column", error.column());
        append(builder, "Value", error.value());
        return builder.toString();
    }

    private static void append(StringBuilder builder, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = value.toString();
        if (text.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(System.lineSeparator());
        }
        builder.append(label).append(": ").append(text);
    }
}
