package com.berk.dataintegration.ui;

import com.berk.dataintegration.excel.ExcelTemplateWriter;
import com.berk.dataintegration.service.ConversionErrorReportWriter;
import com.berk.dataintegration.service.ConversionRecordCounts;
import com.berk.dataintegration.service.ConversionResult;
import com.berk.dataintegration.validation.ValidationError;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ConversionController {
    private final ConversionViewModel model;
    private final ConversionExecutor executor;
    private final ConversionErrorReportWriter errorReportWriter;
    private final ExcelTemplateWriter templateWriter;

    public ConversionController(ConversionViewModel model, ConversionExecutor executor) {
        this(model, executor, new ConversionErrorReportWriter(), new ExcelTemplateWriter());
    }

    ConversionController(
            ConversionViewModel model,
            ConversionExecutor executor,
            ConversionErrorReportWriter errorReportWriter,
            ExcelTemplateWriter templateWriter
    ) {
        this.model = model;
        this.executor = executor;
        this.errorReportWriter = errorReportWriter;
        this.templateWriter = templateWriter;
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
        if (!OutputPathExtensions.hasExtension(request.inputPath(), request.direction().inputExtension())) {
            model.setStatus("Input file must use ." + request.direction().inputExtension() + " extension.");
            model.setErrors(List.of());
            return;
        }
        request = new ConversionRequest(
                request.direction(),
                request.inputPath(),
                OutputPathExtensions.withExpectedExtension(request.outputPath(), request.direction().outputExtension())
        );
        model.setOutputPath(request.outputPath());
        if (samePath(request.inputPath(), request.outputPath())) {
            model.setStatus("Input and output files must be different.");
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
        suggestOutputPathIfEmpty();
    }

    public void updateInputPath(Path inputPath) {
        model.setInputPath(inputPath);
        suggestOutputPathIfEmpty();
    }

    public void saveErrorReport(Path outputPath) {
        if (outputPath == null) {
            model.setStatus("Select an error report output file.");
            return;
        }
        try {
            errorReportWriter.writeCsv(model.errors(), outputPath);
            model.setStatus("Error report saved.");
        } catch (IOException | RuntimeException exception) {
            model.setStatus("Error report could not be saved: " + exception.getMessage());
        }
    }

    public void saveTemplate(Path outputPath) {
        if (outputPath == null) {
            model.setStatus("Select a template output file.");
            return;
        }
        try {
            templateWriter.write(OutputPathExtensions.withExpectedExtension(outputPath, "xlsx"));
            model.setStatus("Excel template saved.");
        } catch (IOException | RuntimeException exception) {
            model.setStatus("Excel template could not be saved: " + exception.getMessage());
        }
    }

    private void suggestOutputPathIfEmpty() {
        if (model.inputPath() != null && model.outputPath() == null) {
            model.setOutputPath(OutputPathExtensions.replaceExtension(model.inputPath(), model.direction().outputExtension()));
        }
    }

    private boolean samePath(Path first, Path second) {
        if (first == null || second == null) {
            return false;
        }
        return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
    }
}
