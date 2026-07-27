package com.berk.dataintegration.service;

import com.berk.dataintegration.domain.InvoiceBatch;

public record ConversionRecordCounts(
        int partyCount,
        int paymentAccountCount,
        int invoiceCount,
        int invoiceLineCount
) {
    public static ConversionRecordCounts empty() {
        return new ConversionRecordCounts(0, 0, 0, 0);
    }

    public static ConversionRecordCounts from(InvoiceBatch batch) {
        int lineCount = batch.invoices().stream()
                .mapToInt(invoice -> invoice.lines().size())
                .sum();
        return new ConversionRecordCounts(
                batch.parties().size(),
                batch.paymentAccounts().size(),
                batch.invoices().size(),
                lineCount
        );
    }
}
