package com.berk.dataintegration.ui;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorSummaryFormatterTest {
    @Test
    void formatsEmptySummary() {
        assertEquals("No errors.", ErrorSummaryFormatter.format(List.of()));
    }

    @Test
    void groupsErrorsByUiCategory() {
        List<ValidationError> errors = List.of(
                error(ValidationCode.HEADER_MISMATCH, ValidationCategory.CONTRACT),
                error(ValidationCode.UNKNOWN_COLUMN, ValidationCategory.CONTRACT),
                error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD),
                error(ValidationCode.XML_SCHEMA_VALIDATION_FAILED, ValidationCategory.CONTRACT)
        );

        String summary = ErrorSummaryFormatter.format(errors);

        assertEquals("Errors: 4   Contract: 2   Field: 1   XML schema: 1", summary);
    }

    @Test
    void treatsXmlSchemaErrorsSeparatelyFromTheirValidationCategory() {
        List<ValidationError> errors = List.of(
                error(ValidationCode.XML_SCHEMA_VALIDATION_FAILED, ValidationCategory.CONTRACT),
                error(ValidationCode.XML_SCHEMA_VALIDATION_FAILED, ValidationCategory.CONTRACT)
        );

        String summary = ErrorSummaryFormatter.format(errors);

        assertEquals("Errors: 2   XML schema: 2", summary);
    }

    private ValidationError error(ValidationCode code, ValidationCategory category) {
        return ValidationError.of(code, category, "Error.", "invoiceNumber", "INV-1");
    }
}
