package com.berk.dataintegration.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ConversionReport(
        Path inputPath,
        Path outputPath,
        ConversionStatus status,
        ConversionRecordCounts recordCounts,
        int errorCount,
        List<ConversionErrorView> errors,
        List<ConversionErrorGroup> errorGroups
) {
    public ConversionReport {
        recordCounts = recordCounts == null ? ConversionRecordCounts.empty() : recordCounts;
        errors = errors == null ? List.of() : List.copyOf(errors);
        errorGroups = errorGroups == null ? List.of() : List.copyOf(errorGroups);
    }

    public static ConversionReport from(ConversionResult result) {
        List<ConversionErrorView> errors = result.errors().stream()
                .map(ConversionErrorView::from)
                .toList();
        return new ConversionReport(
                result.inputPath(),
                result.outputPath(),
                result.isSuccess() ? ConversionStatus.SUCCESS : ConversionStatus.FAILED,
                result.recordCounts(),
                errors.size(),
                errors,
                group(errors)
        );
    }

    private static List<ConversionErrorGroup> group(List<ConversionErrorView> errors) {
        Map<GroupKey, List<ConversionErrorView>> grouped = new LinkedHashMap<>();
        for (ConversionErrorView error : errors) {
            grouped.computeIfAbsent(new GroupKey(error.type(), error.phase()), ignored -> new ArrayList<>()).add(error);
        }
        return grouped.entrySet().stream()
                .map(entry -> new ConversionErrorGroup(
                        entry.getKey().type(),
                        entry.getKey().phase(),
                        entry.getValue().size(),
                        entry.getValue()
                ))
                .toList();
    }

    private record GroupKey(ConversionErrorType type, ConversionErrorPhase phase) {
    }
}
