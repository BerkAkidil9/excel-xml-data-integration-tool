package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionRecordCounts;
import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversionViewModelTest {
    @Test
    void exposesRequestFromCurrentState() {
        ConversionViewModel model = new ConversionViewModel();
        model.setDirection(ConversionDirection.XML_TO_EXCEL);
        model.setInputPath(Path.of("input.xml"));
        model.setOutputPath(Path.of("output.xlsx"));

        ConversionRequest request = model.request();

        assertEquals(ConversionDirection.XML_TO_EXCEL, request.direction());
        assertEquals(Path.of("input.xml"), request.inputPath());
        assertEquals(Path.of("output.xlsx"), request.outputPath());
    }

    @Test
    void defaultsStatusSeverityToInfo() {
        ConversionViewModel model = new ConversionViewModel();

        assertEquals(ConversionStatusSeverity.INFO, model.statusSeverity());
    }

    @Test
    void storesStatusSeverityWithStatusText() {
        ConversionViewModel model = new ConversionViewModel();

        model.setStatus("Failed.", ConversionStatusSeverity.ERROR);

        assertEquals("Failed.", model.status());
        assertEquals(ConversionStatusSeverity.ERROR, model.statusSeverity());
    }

    @Test
    void protectsErrorListFromExternalMutation() {
        ConversionViewModel model = new ConversionViewModel();
        List<ValidationError> errors = new ArrayList<>();
        errors.add(ValidationError.of(
                ValidationCode.REQUIRED_FIELD,
                ValidationCategory.FIELD,
                "Required.",
                "invoiceNumber",
                "INV-1"
        ));

        model.setErrors(errors);
        errors.clear();

        assertEquals(1, model.errors().size());
        assertThrows(UnsupportedOperationException.class, () -> model.errors().clear());
    }

    @Test
    void publishesPropertyChanges() {
        ConversionViewModel model = new ConversionViewModel();
        AtomicInteger changes = new AtomicInteger();
        model.addPropertyChangeListener(event -> changes.incrementAndGet());

        model.setStatus("Working", ConversionStatusSeverity.RUNNING);
        model.setRecordCounts(new ConversionRecordCounts(1, 2, 3, 4));

        assertEquals(3, changes.get());
    }

    @Test
    void resetRestoresInitialUiState() {
        ConversionViewModel model = new ConversionViewModel();
        ValidationError error = ValidationError.of(
                ValidationCode.REQUIRED_FIELD,
                ValidationCategory.FIELD,
                "Missing.",
                "invoiceNumber",
                "INV-1"
        );
        model.setDirection(ConversionDirection.XML_TO_EXCEL);
        model.setInputPath(Path.of("input.xml"));
        model.setOutputPath(Path.of("output.xlsx"));
        model.setRunning(true);
        model.setStatus("Failed.", ConversionStatusSeverity.ERROR);
        model.setRecordCounts(new ConversionRecordCounts(1, 2, 3, 4));
        model.setErrors(List.of(error));

        model.reset();

        assertEquals(ConversionDirection.EXCEL_TO_XML, model.direction());
        assertNull(model.inputPath());
        assertNull(model.outputPath());
        assertFalse(model.running());
        assertEquals("Ready.", model.status());
        assertEquals(ConversionStatusSeverity.INFO, model.statusSeverity());
        assertEquals(ConversionRecordCounts.empty(), model.recordCounts());
        assertEquals(List.of(), model.errors());
    }
}
