package com.berk.dataintegration.calculation;

import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.domain.InvoiceLineAmounts;
import com.berk.dataintegration.domain.InvoiceTotals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class MoneyCalculationService {
    public static final int MONEY_SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public InvoiceLineAmounts calculateLineAmounts(InvoiceLine line) {
        BigDecimal rawNetAmount = line.quantity().multiply(line.unitPrice());
        BigDecimal rawTaxAmount = rawNetAmount.multiply(line.taxRate()).divide(ONE_HUNDRED);
        return new InvoiceLineAmounts(
                line.lineNumber(),
                toMoney(rawNetAmount),
                toMoney(rawTaxAmount)
        );
    }

    public List<InvoiceLineAmounts> calculateLineAmounts(Invoice invoice) {
        return invoice.lines().stream()
                .map(this::calculateLineAmounts)
                .toList();
    }

    public InvoiceTotals calculateInvoiceTotals(Invoice invoice) {
        BigDecimal taxExclusiveAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        BigDecimal taxAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);

        for (InvoiceLineAmounts lineAmounts : calculateLineAmounts(invoice)) {
            taxExclusiveAmount = taxExclusiveAmount.add(lineAmounts.lineNetAmount());
            taxAmount = taxAmount.add(lineAmounts.lineTaxAmount());
        }

        return new InvoiceTotals(
                toMoney(taxExclusiveAmount),
                toMoney(taxAmount),
                toMoney(taxExclusiveAmount.add(taxAmount))
        );
    }

    private BigDecimal toMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING_MODE);
    }
}
