package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionResult;

import java.util.function.Consumer;

public interface ConversionExecutor {
    void execute(
            ConversionRequest request,
            Consumer<ConversionResult> onComplete,
            Consumer<Exception> onError
    );
}
