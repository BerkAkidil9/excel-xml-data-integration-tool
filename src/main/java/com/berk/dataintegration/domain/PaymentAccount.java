package com.berk.dataintegration.domain;

public record PaymentAccount(
        String paymentAccountId,
        String accountHolderName,
        String bankName,
        String iban,
        String swiftCode,
        String currencyCode
) {
    public PaymentAccount {
        paymentAccountId = DomainText.trim(paymentAccountId);
        accountHolderName = DomainText.trim(accountHolderName);
        bankName = DomainText.trim(bankName);
        iban = DomainText.trim(iban);
        swiftCode = DomainText.optional(swiftCode);
        currencyCode = DomainText.trim(currencyCode);
    }
}
