package com.berk.dataintegration.ui;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationError;

enum ErrorCategoryFilter {
    ALL("All", null),
    CONTRACT("Contract", ValidationCategory.CONTRACT),
    FIELD("Field", ValidationCategory.FIELD),
    PARSE("Parse", ValidationCategory.PARSE),
    REFERENCE("Reference", ValidationCategory.REFERENCE),
    BUSINESS_RULE("Business Rule", ValidationCategory.BUSINESS_RULE),
    IO("I/O", ValidationCategory.IO);

    private final String label;
    private final ValidationCategory category;

    ErrorCategoryFilter(String label, ValidationCategory category) {
        this.label = label;
        this.category = category;
    }

    boolean accepts(ValidationError error) {
        return category == null || error != null && error.category() == category;
    }

    @Override
    public String toString() {
        return label;
    }
}
