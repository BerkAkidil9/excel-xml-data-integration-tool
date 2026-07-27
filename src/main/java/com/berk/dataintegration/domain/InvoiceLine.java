package com.berk.dataintegration.domain;

import java.math.BigDecimal;

public record InvoiceLine(
        int lineNumber,
        String itemCode,
        String description,
        BigDecimal quantity,
        String unitCode,
        BigDecimal unitPrice,
        BigDecimal taxRate
) {
    public InvoiceLine {
        itemCode = DomainText.optional(itemCode);
        description = DomainText.trim(description);
        unitCode = DomainText.trim(unitCode);
    }
}
