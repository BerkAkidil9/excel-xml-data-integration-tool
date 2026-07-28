package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionRecordCounts;
import com.berk.dataintegration.service.ConversionResult;
import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversionControllerTest {
    @TempDir
    Path tempDir;

    @Test
    void requiresInputAndOutputBeforeStarting() {
        ConversionViewModel model = new ConversionViewModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);

        controller.convert();

        assertFalse(model.running());
        assertEquals("Select input and output files.", model.status());
        assertEquals(ConversionStatusSeverity.ERROR, model.statusSeverity());
        assertEquals(0, executor.calls);
    }

    @Test
    void startsConversionAndPassesRequestToExecutor() {
        ConversionViewModel model = readyModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);

        controller.convert();

        assertTrue(model.running());
        assertEquals("Converting...", model.status());
        assertEquals(ConversionStatusSeverity.RUNNING, model.statusSeverity());
        assertEquals(1, executor.calls);
        assertEquals(ConversionDirection.EXCEL_TO_XML, executor.request.direction());
        assertEquals(Path.of("input.xlsx"), executor.request.inputPath());
        assertEquals(Path.of("output.xml"), executor.request.outputPath());
    }

    @Test
    void appendsExpectedOutputExtensionBeforeStartingConversion() {
        ConversionViewModel model = new ConversionViewModel();
        model.setDirection(ConversionDirection.XML_TO_EXCEL);
        model.setInputPath(Path.of("input.xml"));
        model.setOutputPath(Path.of("converted-output"));
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);

        controller.convert();

        assertEquals(Path.of("converted-output.xlsx"), executor.request.outputPath());
        assertEquals(Path.of("converted-output.xlsx"), model.outputPath());
    }

    @Test
    void suggestsOutputPathWhenInputIsSelectedAndOutputIsEmpty() {
        ConversionViewModel model = new ConversionViewModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);

        controller.updateInputPath(Path.of("invoice.xlsx"));

        assertEquals(Path.of("invoice.xlsx"), model.inputPath());
        assertEquals(Path.of("invoice.xml"), model.outputPath());
    }

    @Test
    void updatesSuggestedOutputPathWhenInputChanges() {
        ConversionViewModel model = new ConversionViewModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);
        controller.updateInputPath(Path.of("first.xlsx"));

        controller.updateInputPath(Path.of("second.xlsx"));

        assertEquals(Path.of("second.xlsx"), model.inputPath());
        assertEquals(Path.of("second.xml"), model.outputPath());
    }

    @Test
    void keepsExistingOutputPathWhenInputChanges() {
        ConversionViewModel model = new ConversionViewModel();
        model.setOutputPath(Path.of("custom.xml"));
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);

        controller.updateInputPath(Path.of("invoice.xlsx"));

        assertEquals(Path.of("custom.xml"), model.outputPath());
    }

    @Test
    void clearsSuggestedOutputPathWhenInputIsCleared() {
        ConversionViewModel model = new ConversionViewModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);
        controller.updateInputPath(Path.of("invoice.xlsx"));

        controller.updateInputPath(null);

        assertNull(model.inputPath());
        assertNull(model.outputPath());
    }

    @Test
    void updatesSuggestedOutputPathWhenDirectionChanges() {
        ConversionViewModel model = new ConversionViewModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);
        controller.updateInputPath(Path.of("invoice.xlsx"));

        controller.updateDirection(ConversionDirection.XML_TO_EXCEL);

        assertEquals(Path.of("invoice.xlsx"), model.inputPath());
        assertEquals(Path.of("invoice.xlsx"), model.outputPath());
    }

    @Test
    void rejectsInputFileWithUnexpectedExtensionBeforeStarting() {
        ConversionViewModel model = new ConversionViewModel();
        model.setInputPath(Path.of("input.xml"));
        model.setOutputPath(Path.of("output.xml"));
        model.setRecordCounts(new ConversionRecordCounts(9, 9, 9, 9));
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);

        controller.convert();

        assertFalse(model.running());
        assertEquals("Input file must use .xlsx extension.", model.status());
        assertEquals(ConversionStatusSeverity.ERROR, model.statusSeverity());
        assertEquals(ConversionRecordCounts.empty(), model.recordCounts());
        assertEquals(0, executor.calls);
    }

    @Test
    void successfulConversionUpdatesCountsAndStatus() {
        ConversionViewModel model = readyModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);
        controller.convert();

        executor.complete(new ConversionResult(
                Path.of("input.xlsx"),
                Path.of("output.xml"),
                new ConversionRecordCounts(2, 1, 3, 4),
                List.of()
        ));

        assertFalse(model.running());
        assertEquals("Conversion completed.", model.status());
        assertEquals(ConversionStatusSeverity.SUCCESS, model.statusSeverity());
        assertEquals(new ConversionRecordCounts(2, 1, 3, 4), model.recordCounts());
        assertTrue(model.errors().isEmpty());
    }

    @Test
    void conversionErrorsAreDisplayedAsStructuredErrors() {
        ConversionViewModel model = readyModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);
        controller.convert();
        ValidationError error = ValidationError.of(
                ValidationCode.REQUIRED_FIELD,
                ValidationCategory.FIELD,
                "Missing.",
                "invoiceNumber",
                "INV-1"
        );

        executor.complete(new ConversionResult(
                Path.of("input.xlsx"),
                Path.of("output.xml"),
                ConversionRecordCounts.empty(),
                List.of(error)
        ));

        assertFalse(model.running());
        assertEquals("Conversion failed with 1 error(s).", model.status());
        assertEquals(ConversionStatusSeverity.ERROR, model.statusSeverity());
        assertEquals(List.of(error), model.errors());
    }

    @Test
    void unexpectedExecutorFailureClearsRunningState() {
        ConversionViewModel model = readyModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);
        controller.convert();

        executor.fail(new IllegalStateException("boom"));

        assertFalse(model.running());
        assertEquals("Conversion failed unexpectedly: boom", model.status());
        assertEquals(ConversionStatusSeverity.ERROR, model.statusSeverity());
    }

    @Test
    void savesCurrentErrorsAsCsvReport() throws IOException {
        ConversionViewModel model = readyModel();
        ValidationError error = ValidationError.of(
                ValidationCode.REQUIRED_FIELD,
                ValidationCategory.FIELD,
                "Missing.",
                "invoiceNumber",
                "INV-1"
        );
        model.setErrors(List.of(error));
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);
        Path report = tempDir.resolve("errors.csv");

        controller.saveErrorReport(report);

        assertEquals("Error report saved.", model.status());
        assertEquals(ConversionStatusSeverity.SUCCESS, model.statusSeverity());
        String csv = Files.readString(report, StandardCharsets.UTF_8);
        assertTrue(csv.contains("REQUIRED_FIELD,FIELD,invoiceNumber,INV-1"), csv);
    }

    @Test
    void savesExcelTemplate() {
        ConversionViewModel model = readyModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);
        Path template = tempDir.resolve("invoice-template");

        controller.saveTemplate(template);

        assertEquals("Excel template saved.", model.status());
        assertEquals(ConversionStatusSeverity.SUCCESS, model.statusSeverity());
        assertTrue(Files.exists(tempDir.resolve("invoice-template.xlsx")));
    }

    @Test
    void resetClearsFinishedConversionState() {
        ConversionViewModel model = readyModel();
        ValidationError error = ValidationError.of(
                ValidationCode.REQUIRED_FIELD,
                ValidationCategory.FIELD,
                "Missing.",
                "invoiceNumber",
                "INV-1"
        );
        model.setDirection(ConversionDirection.XML_TO_EXCEL);
        model.setStatus("Conversion failed with 1 error(s).", ConversionStatusSeverity.ERROR);
        model.setRecordCounts(new ConversionRecordCounts(1, 2, 3, 4));
        model.setErrors(List.of(error));
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);

        controller.reset();

        assertEquals(ConversionDirection.EXCEL_TO_XML, model.direction());
        assertNull(model.inputPath());
        assertNull(model.outputPath());
        assertFalse(model.running());
        assertEquals("Ready.", model.status());
        assertEquals(ConversionStatusSeverity.INFO, model.statusSeverity());
        assertEquals(ConversionRecordCounts.empty(), model.recordCounts());
        assertTrue(model.errors().isEmpty());
    }

    @Test
    void resetDoesNotClearStateDuringRunningConversion() {
        ConversionViewModel model = readyModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);
        controller.convert();

        controller.reset();

        assertTrue(model.running());
        assertEquals(Path.of("input.xlsx"), model.inputPath());
        assertEquals(Path.of("output.xml"), model.outputPath());
        assertEquals("Converting...", model.status());
        assertEquals(ConversionStatusSeverity.RUNNING, model.statusSeverity());
    }

    private ConversionViewModel readyModel() {
        ConversionViewModel model = new ConversionViewModel();
        model.setInputPath(Path.of("input.xlsx"));
        model.setOutputPath(Path.of("output.xml"));
        return model;
    }

    private static final class RecordingExecutor implements ConversionExecutor {
        private int calls;
        private ConversionRequest request;
        private Consumer<ConversionResult> onComplete;
        private Consumer<Exception> onError;

        @Override
        public void execute(
                ConversionRequest request,
                Consumer<ConversionResult> onComplete,
                Consumer<Exception> onError
        ) {
            calls++;
            this.request = request;
            this.onComplete = onComplete;
            this.onError = onError;
        }

        void complete(ConversionResult result) {
            onComplete.accept(result);
        }

        void fail(Exception exception) {
            onError.accept(exception);
        }
    }
}
