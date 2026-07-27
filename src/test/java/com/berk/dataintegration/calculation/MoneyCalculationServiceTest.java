package com.berk.dataintegration.calculation;

import com.berk.dataintegration.TestBatches;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.domain.InvoiceLineAmounts;
import com.berk.dataintegration.domain.InvoiceTotals;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyCalculationServiceTest {
    private final MoneyCalculationService service = new MoneyCalculationService();

    @Test
    void calculatesLineAndInvoiceTotalsForOneLine() {
        Invoice invoice = TestBatches.invoice(
                "INV-2026-0001",
                "SUP-001",
                "CUS-001",
                "PAY-001",
                "TRY",
                List.of(TestBatches.line(1))
        );

        InvoiceLineAmounts lineAmounts = service.calculateLineAmounts(invoice.lines().getFirst());
        InvoiceTotals totals = service.calculateInvoiceTotals(invoice);

        assertEquals(new BigDecimal("2500.00"), lineAmounts.lineNetAmount());
        assertEquals(new BigDecimal("500.00"), lineAmounts.lineTaxAmount());
        assertEquals(new BigDecimal("2500.00"), totals.taxExclusiveAmount());
        assertEquals(new BigDecimal("500.00"), totals.taxAmount());
        assertEquals(new BigDecimal("3000.00"), totals.payableAmount());
    }

    @Test
    void sumsRoundedValuesForMultipleLineTotals() {
        Invoice invoice = TestBatches.invoice(
                "INV-2026-0002",
                "SUP-001",
                "CUS-001",
                "PAY-001",
                "TRY",
                List.of(
                        TestBatches.line(1, "2", "1250.00", "20.00"),
                        TestBatches.line(2, "3", "10.105", "10.00")
                )
        );

        InvoiceTotals totals = service.calculateInvoiceTotals(invoice);

        assertEquals(new BigDecimal("2530.32"), totals.taxExclusiveAmount());
        assertEquals(new BigDecimal("503.03"), totals.taxAmount());
        assertEquals(new BigDecimal("3033.35"), totals.payableAmount());
    }

    @Test
    void roundsHalfUpAtLineBoundary() {
        InvoiceLine line = TestBatches.line(1, "1", "0.005", "0.00");

        InvoiceLineAmounts amounts = service.calculateLineAmounts(line);

        assertEquals(new BigDecimal("0.01"), amounts.lineNetAmount());
        assertEquals(new BigDecimal("0.00"), amounts.lineTaxAmount());
    }

    @Test
    void roundsTaxHalfUpAtLineBoundary() {
        InvoiceLine line = TestBatches.line(1, "1", "0.025", "20.00");

        InvoiceLineAmounts amounts = service.calculateLineAmounts(line);

        assertEquals(new BigDecimal("0.03"), amounts.lineNetAmount());
        assertEquals(new BigDecimal("0.01"), amounts.lineTaxAmount());
    }
}
