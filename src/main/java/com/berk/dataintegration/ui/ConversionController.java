package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionRecordCounts;
import com.berk.dataintegration.service.ConversionResult;
import com.berk.dataintegration.validation.ValidationError;

import java.util.List;

public final class ConversionController {
    private final ConversionViewModel model;
    private final ConversionExecutor executor;

    public ConversionController(ConversionViewModel model, ConversionExecutor executor) {
        this.model = model;
        this.executor = executor;
    }

    public void convert() {
        if (model.running()) {
            return;
        }
        ConversionRequest request = model.request();
        if (request.inputPath() == null || request.outputPath() == null) {
            model.setStatus("Select input and output files.");
            model.setErrors(List.of());
            return;
        }

        model.setRunning(true);
        model.setStatus("Converting...");
        model.setRecordCounts(ConversionRecordCounts.empty());
        model.setErrors(List.of());
        executor.execute(request, this::applyResult, this::applyUnexpectedError);
    }

    private void applyResult(ConversionResult result) {
        model.setRunning(false);
        model.setRecordCounts(result.recordCounts());
        model.setErrors(result.errors());
        model.setStatus(result.isSuccess()
                ? "Conversion completed."
                : "Conversion failed with " + result.errors().size() + " error(s).");
    }

    private void applyUnexpectedError(Exception exception) {
        model.setRunning(false);
        model.setStatus("Conversion failed unexpectedly: " + exception.getMessage());
    }

    public void updateDirection(ConversionDirection direction) {
        model.setDirection(direction);
    }
}
