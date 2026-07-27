package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionRecordCounts;
import com.berk.dataintegration.service.ConversionResult;
import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversionControllerTest {
    @Test
    void requiresInputAndOutputBeforeStarting() {
        ConversionViewModel model = new ConversionViewModel();
        RecordingExecutor executor = new RecordingExecutor();
        ConversionController controller = new ConversionController(model, executor);

        controller.convert();

        assertFalse(model.running());
        assertEquals("Select input and output files.", model.status());
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
        assertEquals(1, executor.calls);
        assertEquals(ConversionDirection.EXCEL_TO_XML, executor.request.direction());
        assertEquals(Path.of("input.xlsx"), executor.request.inputPath());
        assertEquals(Path.of("output.xml"), executor.request.outputPath());
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
