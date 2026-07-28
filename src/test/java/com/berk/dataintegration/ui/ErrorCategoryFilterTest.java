package com.berk.dataintegration.ui;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorCategoryFilterTest {
    @Test
    void allFilterAcceptsEveryError() {
        assertTrue(ErrorCategoryFilter.ALL.accepts(error(ValidationCategory.FIELD)));
        assertTrue(ErrorCategoryFilter.ALL.accepts(error(ValidationCategory.BUSINESS_RULE)));
    }

    @Test
    void categoryFilterAcceptsOnlyMatchingCategory() {
        assertTrue(ErrorCategoryFilter.FIELD.accepts(error(ValidationCategory.FIELD)));
        assertFalse(ErrorCategoryFilter.FIELD.accepts(error(ValidationCategory.CONTRACT)));
        assertFalse(ErrorCategoryFilter.FIELD.accepts(null));
    }

    private ValidationError error(ValidationCategory category) {
        return ValidationError.of(ValidationCode.REQUIRED_FIELD, category, "Missing.", "invoiceNumber", "INV-1");
    }
}
