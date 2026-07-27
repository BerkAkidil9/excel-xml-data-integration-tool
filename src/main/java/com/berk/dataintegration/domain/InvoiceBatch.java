package com.berk.dataintegration.domain;

import java.util.List;

public record InvoiceBatch(
        List<Party> parties,
        List<PaymentAccount> paymentAccounts,
        List<Invoice> invoices
) {
    public InvoiceBatch {
        parties = parties == null ? List.of() : List.copyOf(parties);
        paymentAccounts = paymentAccounts == null ? List.of() : List.copyOf(paymentAccounts);
        invoices = invoices == null ? List.of() : List.copyOf(invoices);
    }
}
