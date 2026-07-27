package com.berk.dataintegration.service;

import com.berk.dataintegration.validation.ValidationError;

import java.nio.file.Path;
import java.util.List;

public record ConversionResult(
        Path inputPath,
        Path outputPath,
        ConversionRecordCounts recordCounts,
        List<ValidationError> errors
) {
    public ConversionResult {
        recordCounts = recordCounts == null ? ConversionRecordCounts.empty() : recordCounts;
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }
}
