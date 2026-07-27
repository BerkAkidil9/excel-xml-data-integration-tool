package com.berk.dataintegration.domain;

import java.time.LocalDate;
import java.util.List;

public record Invoice(
        String invoiceNumber,
        LocalDate issueDate,
        LocalDate dueDate,
        String currencyCode,
        String supplierId,
        String customerId,
        String paymentAccountId,
        String note,
        List<InvoiceLine> lines
) {
    public Invoice {
        invoiceNumber = DomainText.trim(invoiceNumber);
        currencyCode = DomainText.trim(currencyCode);
        supplierId = DomainText.trim(supplierId);
        customerId = DomainText.trim(customerId);
        paymentAccountId = DomainText.trim(paymentAccountId);
        note = DomainText.optional(note);
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
