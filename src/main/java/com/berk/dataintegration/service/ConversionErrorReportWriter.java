package com.berk.dataintegration.service;

import com.berk.dataintegration.validation.ValidationError;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConversionErrorReportWriter {
    private static final List<String> SUMMARY_HEADERS = List.of("metric", "value");
    private static final List<String> GROUP_HEADERS = List.of(
            "code",
            "category",
            "field",
            "source_file",
            "sheet_or_path",
            "count",
            "message"
    );
    private static final List<String> DETAIL_HEADERS = List.of(
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
        List<ValidationError> safeErrors = errors == null ? List.of() : List.copyOf(errors);
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writeSummary(writer, safeErrors);
            writer.newLine();
            writeGroupedSummary(writer, safeErrors);
            writer.newLine();
            writeDetails(writer, safeErrors);
        }
    }

    private void writeSummary(BufferedWriter writer, List<ValidationError> errors) throws IOException {
        writer.write("summary");
        writer.newLine();
        writeRow(writer, SUMMARY_HEADERS);
        writeRow(writer, List.of("total_errors", text(errors.size())));
    }

    private void writeGroupedSummary(BufferedWriter writer, List<ValidationError> errors) throws IOException {
        writer.write("summary_by_error");
        writer.newLine();
        writeRow(writer, GROUP_HEADERS);
        for (Map.Entry<ErrorGroupKey, Integer> entry : groupedErrors(errors).entrySet()) {
            ErrorGroupKey key = entry.getKey();
            writeRow(writer, List.of(
                    text(key.code()),
                    text(key.category()),
                    text(key.field()),
                    text(key.sourceFile()),
                    text(key.sheetOrPath()),
                    text(entry.getValue()),
                    text(key.message())
            ));
        }
    }

    private Map<ErrorGroupKey, Integer> groupedErrors(List<ValidationError> errors) {
        Map<ErrorGroupKey, Integer> groups = new LinkedHashMap<>();
        for (ValidationError error : errors) {
            ErrorGroupKey key = ErrorGroupKey.from(error);
            groups.merge(key, 1, Integer::sum);
        }
        return groups;
    }

    private void writeDetails(BufferedWriter writer, List<ValidationError> errors) throws IOException {
        writer.write("details");
        writer.newLine();
        writeRow(writer, DETAIL_HEADERS);
        for (ValidationError error : errors) {
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

    private record ErrorGroupKey(
            Object code,
            Object category,
            String field,
            String sourceFile,
            String sheetOrPath,
            String message
    ) {
        private static ErrorGroupKey from(ValidationError error) {
            return new ErrorGroupKey(
                    error.code(),
                    error.category(),
                    error.field(),
                    error.sourceFile(),
                    error.sheet(),
                    error.message()
            );
        }
    }
}
