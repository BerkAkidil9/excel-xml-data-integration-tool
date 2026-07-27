package com.berk.dataintegration.xml;

import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.validation.ValidationError;

import java.util.List;

public record XmlReadResult(
        InvoiceBatch batch,
        List<ValidationError> errors
) {
    public XmlReadResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
