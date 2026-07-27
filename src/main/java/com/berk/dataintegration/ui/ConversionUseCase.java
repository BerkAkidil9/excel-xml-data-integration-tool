package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionResult;

@FunctionalInterface
public interface ConversionUseCase {
    ConversionResult convert(ConversionRequest request);
}
