package com.berk.dataintegration.service;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversionReportTest {
    @Test
    void successReportContainsCountsStatusAndNoErrors() {
        ConversionResult result = new ConversionResult(
                Path.of("input.xlsx"),
                Path.of("output.xml"),
                new ConversionRecordCounts(2, 1, 3, 4),
                List.of()
        );

        ConversionReport report = result.toReport();

        assertEquals(ConversionStatus.SUCCESS, report.status());
        assertEquals(new ConversionRecordCounts(2, 1, 3, 4), report.recordCounts());
        assertEquals(0, report.errorCount());
        assertTrue(report.errors().isEmpty());
        assertTrue(report.errorGroups().isEmpty());
    }

    @Test
    void failureReportPreservesOrderedErrorsAndGroupsByPresentationType() {
        ValidationError parseError = new ValidationError(
                ValidationCode.INVALID_DECIMAL_FORMAT,
                ValidationCategory.PARSE,
                "Decimal value is invalid.",
                "quantity",
                "INV-001#1",
                "input.xlsx",
                "InvoiceLines",
                2,
                "E",
                "1.234,56"
        );
        ValidationError xsdError = new ValidationError(
                ValidationCode.XML_SCHEMA_VALIDATION_FAILED,
                ValidationCategory.CONTRACT,
                "XML input does not conform to invoice-data-v1.xsd.",
                null,
                null,
                "input.xml",
                null,
                18,
                "42",
                "cvc-complex-type"
        );
        ValidationError businessError = new ValidationError(
                ValidationCode.CURRENCY_MISMATCH,
                ValidationCategory.BUSINESS_RULE,
                "Invoice currency must equal payment account currency.",
                "currencyCode",
                "INV-001",
                "input.xlsx",
                "Invoices",
                2,
                "D",
                "EUR"
        );
        ValidationError outputError = new ValidationError(
                ValidationCode.CONVERSION_OUTPUT_FAILED,
                ValidationCategory.IO,
                "Conversion output could not be written.",
                null,
                null,
                "output.xml",
                null,
                null,
                null,
                "Permission denied"
        );

        ConversionReport report = new ConversionResult(
                Path.of("input.xlsx"),
                Path.of("output.xml"),
                new ConversionRecordCounts(2, 1, 1, 1),
                List.of(parseError, xsdError, businessError, outputError)
        ).toReport();

        assertEquals(ConversionStatus.FAILED, report.status());
        assertEquals(4, report.errorCount());
        assertEquals(List.of(
                ValidationCode.INVALID_DECIMAL_FORMAT,
                ValidationCode.XML_SCHEMA_VALIDATION_FAILED,
                ValidationCode.CURRENCY_MISMATCH,
                ValidationCode.CONVERSION_OUTPUT_FAILED
        ), report.errors().stream().map(ConversionErrorView::code).toList());

        ConversionErrorView first = report.errors().getFirst();
        assertEquals(ValidationCategory.PARSE, first.category());
        assertEquals(ConversionErrorPhase.INPUT, first.phase());
        assertEquals(ConversionErrorType.PARSE, first.type());
        assertEquals("input.xlsx", first.sourceFile());
        assertEquals("InvoiceLines", first.sheetOrPath());
        assertEquals(2, first.row());
        assertEquals("E", first.column());
        assertEquals("quantity", first.field());
        assertEquals("INV-001#1", first.recordId());
        assertEquals("1.234,56", first.value());
        assertEquals("input.xlsx / InvoiceLines / row 2 / column E", first.location());

        assertEquals(List.of(
                ConversionErrorType.PARSE,
                ConversionErrorType.XSD,
                ConversionErrorType.BUSINESS_VALIDATION,
                ConversionErrorType.IO
        ), report.errorGroups().stream().map(ConversionErrorGroup::type).toList());
        assertEquals(List.of(
                ConversionErrorPhase.INPUT,
                ConversionErrorPhase.VALIDATION,
                ConversionErrorPhase.VALIDATION,
                ConversionErrorPhase.OUTPUT
        ), report.errorGroups().stream().map(ConversionErrorGroup::phase).toList());
        assertEquals(List.of(1, 1, 1, 1), report.errorGroups().stream().map(ConversionErrorGroup::count).toList());
    }
}
