package com.berk.dataintegration.domain;

import java.math.BigDecimal;

public record InvoiceLineAmounts(
        int lineNumber,
        BigDecimal lineNetAmount,
        BigDecimal lineTaxAmount
) {
}
