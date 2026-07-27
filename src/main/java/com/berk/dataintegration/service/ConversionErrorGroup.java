package com.berk.dataintegration.service;

import java.util.List;

public record ConversionErrorGroup(
        ConversionErrorType type,
        ConversionErrorPhase phase,
        int count,
        List<ConversionErrorView> errors
) {
    public ConversionErrorGroup {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
