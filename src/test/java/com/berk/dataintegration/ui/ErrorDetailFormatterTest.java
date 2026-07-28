package com.berk.dataintegration.ui;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ErrorDetailFormatterTest {
    @Test
    void formatsOnlyPresentStructuredFields() {
        ValidationError error = new ValidationError(
                ValidationCode.REQUIRED_FIELD,
                ValidationCategory.FIELD,
                "Missing.",
                "invoiceNumber",
                "INV-1",
                "input.xlsx",
                "Invoices",
                2,
                "A",
                "bad"
        );

        String detail = ErrorDetailFormatter.format(error);

        assertEquals("""
                Code: REQUIRED_FIELD
                Category: FIELD
                Message: Missing.
                Field: invoiceNumber
                Record: INV-1
                Source: input.xlsx
                Sheet/Path: Invoices
                Row: 2
                Column: A
                Value: bad""", detail);
    }

    @Test
    void omitsBlankOptionalValues() {
        ValidationError error = new ValidationError(
                ValidationCode.REQUIRED_FIELD,
                ValidationCategory.FIELD,
                "Missing.",
                null,
                "",
                null,
                null,
                null,
                null,
                null
        );

        String detail = ErrorDetailFormatter.format(error);

        assertFalse(detail.contains("Field:"));
        assertFalse(detail.contains("Record:"));
        assertEquals("""
                Code: REQUIRED_FIELD
                Category: FIELD
                Message: Missing.""", detail);
    }

    @Test
    void nullErrorFormatsAsEmptyText() {
        assertEquals("", ErrorDetailFormatter.format(null));
    }
}
