package com.berk.dataintegration.service;

import com.berk.dataintegration.validation.ValidationError;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConversionErrorReportWriter {
    private static final List<String> HEADERS = List.of(
            "code",
            "category",
            "field",
            "record_id",
            "source_file",
            "sheet_or_path",
            "row",
            "column",
            "value",
            "message"
    );

    public void writeCsv(List<ValidationError> errors, Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writeRow(writer, HEADERS);
            for (ValidationError error : errors == null ? List.<ValidationError>of() : errors) {
                writeRow(writer, List.of(
                        text(error.code()),
                        text(error.category()),
                        text(error.field()),
                        text(error.recordId()),
                        text(error.sourceFile()),
                        text(error.sheet()),
                        text(error.row()),
                        text(error.column()),
                        text(error.value()),
                        text(error.message())
                ));
            }
        }
    }

    private void writeRow(BufferedWriter writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escape(values.get(i)));
        }
        writer.newLine();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
