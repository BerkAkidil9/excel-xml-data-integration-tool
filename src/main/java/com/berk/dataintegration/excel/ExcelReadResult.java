package com.berk.dataintegration.excel;

import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.validation.ValidationError;

import java.util.List;

public record ExcelReadResult(InvoiceBatch batch, List<ValidationError> errors) {
    public ExcelReadResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
