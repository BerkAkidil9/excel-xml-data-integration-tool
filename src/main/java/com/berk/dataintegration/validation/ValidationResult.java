package com.berk.dataintegration.validation;

import java.util.List;

public record ValidationResult(List<ValidationError> errors) {
    public ValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean hasError(ValidationCode code) {
        return errors.stream().anyMatch(error -> error.code() == code);
    }
}
