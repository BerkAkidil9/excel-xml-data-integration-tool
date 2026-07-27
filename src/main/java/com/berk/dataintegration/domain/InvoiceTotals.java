package com.berk.dataintegration.domain;

import java.math.BigDecimal;

public record InvoiceTotals(
        BigDecimal taxExclusiveAmount,
        BigDecimal taxAmount,
        BigDecimal payableAmount
) {
}
