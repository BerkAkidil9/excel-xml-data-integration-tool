package com.berk.dataintegration.excel;

import com.berk.dataintegration.validation.ValidationError;

import java.util.HashMap;
import java.util.Map;

final class ExcelSourceIndex {
    private final Map<String, ExcelSourceLocation> byRecordAndField = new HashMap<>();
    private final Map<String, ExcelSourceLocation> byRecord = new HashMap<>();

    void put(String recordId, String field, ExcelSourceLocation location) {
        if (recordId == null || recordId.isBlank() || location == null) {
            return;
        }
        byRecord.put(recordId, location);
        if (field != null && !field.isBlank()) {
            byRecordAndField.put(key(recordId, field), location);
        }
    }

    ValidationError enrich(ValidationError error) {
        if (error.sourceFile() != null || error.recordId() == null) {
            return error;
        }
        ExcelSourceLocation location = byRecordAndField.get(key(error.recordId(), error.field()));
        if (location == null) {
            location = byRecord.get(error.recordId());
        }
        if (location == null) {
            return error;
        }
        return new ValidationError(
                error.code(),
                error.category(),
                error.message(),
                error.field(),
                error.recordId(),
                location.sourceFile(),
                location.sheet(),
                location.row(),
                location.column(),
                location.value()
        );
    }

    private String key(String recordId, String field) {
        return recordId + "\u0000" + field;
    }
}
