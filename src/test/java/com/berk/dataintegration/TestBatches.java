package com.berk.dataintegration;

import com.berk.dataintegration.domain.Address;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.domain.Party;
import com.berk.dataintegration.domain.PartyRole;
import com.berk.dataintegration.domain.PaymentAccount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TestBatches {
    private TestBatches() {
    }

    public static InvoiceBatch validBatch() {
        return new InvoiceBatch(
                List.of(supplier(), customer()),
                List.of(paymentAccount("PAY-001", "TRY")),
                List.of(invoice("INV-2026-0001", "SUP-001", "CUS-001", "PAY-001", "TRY", List.of(line(1))))
        );
    }

    public static Party supplier() {
        return new Party(
                "SUP-001",
                PartyRole.SUPPLIER,
                "Example Supplier Ltd.",
                "1234567890",
                "info@supplier.example",
                "+90 212 000 00 00",
                new Address("Teknoloji Cad. No:10", "Istanbul", "34000", "TR")
        );
    }

    public static Party customer() {
        return customer("CUS-001");
    }

    public static Party customer(String partyId) {
        return new Party(
                partyId,
                PartyRole.CUSTOMER,
                "Example Customer A.S.",
                null,
                "billing@customer.example",
                null,
                new Address(null, "Ankara", null, "TR")
        );
    }

    public static PaymentAccount paymentAccount(String paymentAccountId, String currencyCode) {
        return new PaymentAccount(
                paymentAccountId,
                "Example Supplier Ltd.",
                "Example Bank",
                "TR00TEST000000000000000000",
                "TESTTRIS",
                currencyCode
        );
    }

    public static Invoice invoice(
            String invoiceNumber,
            String supplierId,
            String customerId,
            String paymentAccountId,
            String currencyCode,
            List<InvoiceLine> lines
    ) {
        return new Invoice(
                invoiceNumber,
                LocalDate.of(2026, 7, 27),
                LocalDate.of(2026, 8, 26),
                currencyCode,
                supplierId,
                customerId,
                paymentAccountId,
                "Temmuz danismanlik hizmeti",
                lines
        );
    }

    public static InvoiceLine line(int lineNumber) {
        return line(lineNumber, "2", "1250.00", "20.00");
    }

    public static InvoiceLine line(int lineNumber, String quantity, String unitPrice, String taxRate) {
        return new InvoiceLine(
                lineNumber,
                "SRV-001",
                "Yazilim gelistirme hizmeti",
                new BigDecimal(quantity),
                "C62",
                new BigDecimal(unitPrice),
                new BigDecimal(taxRate)
        );
    }
}
