package com.berk.dataintegration.xml;

import com.berk.dataintegration.calculation.MoneyCalculationService;
import com.berk.dataintegration.domain.Address;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.domain.InvoiceLineAmounts;
import com.berk.dataintegration.domain.InvoiceTotals;
import com.berk.dataintegration.domain.Party;
import com.berk.dataintegration.domain.PaymentAccount;
import com.berk.dataintegration.xml.dto.XmlInvoiceBatch;

import java.util.List;

public final class XmlInvoiceBatchMapper {
    private final MoneyCalculationService calculationService;

    public XmlInvoiceBatchMapper() {
        this(new MoneyCalculationService());
    }

    XmlInvoiceBatchMapper(MoneyCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    public XmlInvoiceBatch toXml(InvoiceBatch batch) {
        return new XmlInvoiceBatch(
                new XmlInvoiceBatch.PartiesXml(batch.parties().stream().map(this::toXml).toList()),
                new XmlInvoiceBatch.PaymentAccountsXml(batch.paymentAccounts().stream().map(this::toXml).toList()),
                new XmlInvoiceBatch.InvoicesXml(batch.invoices().stream().map(this::toXml).toList())
        );
    }

    private XmlInvoiceBatch.PartyXml toXml(Party party) {
        return new XmlInvoiceBatch.PartyXml(
                party.partyId(),
                party.role() == null ? null : party.role().name(),
                party.name(),
                optional(party.taxNumber()),
                optional(party.email()),
                optional(party.phone()),
                toXml(party.address())
        );
    }

    private XmlInvoiceBatch.AddressXml toXml(Address address) {
        if (address == null) {
            return null;
        }
        return new XmlInvoiceBatch.AddressXml(
                optional(address.street()),
                address.city(),
                optional(address.postalCode()),
                address.countryCode()
        );
    }

    private XmlInvoiceBatch.PaymentAccountXml toXml(PaymentAccount paymentAccount) {
        return new XmlInvoiceBatch.PaymentAccountXml(
                paymentAccount.paymentAccountId(),
                paymentAccount.accountHolderName(),
                paymentAccount.bankName(),
                paymentAccount.iban(),
                optional(paymentAccount.swiftCode()),
                paymentAccount.currencyCode()
        );
    }

    private XmlInvoiceBatch.InvoiceXml toXml(Invoice invoice) {
        return new XmlInvoiceBatch.InvoiceXml(
                invoice.invoiceNumber(),
                invoice.issueDate(),
                invoice.dueDate(),
                invoice.currencyCode(),
                invoice.supplierId(),
                invoice.customerId(),
                invoice.paymentAccountId(),
                optional(invoice.note()),
                new XmlInvoiceBatch.LinesXml(toXmlLines(invoice)),
                toXml(calculationService.calculateInvoiceTotals(invoice))
        );
    }

    private List<XmlInvoiceBatch.LineXml> toXmlLines(Invoice invoice) {
        return invoice.lines().stream()
                .map(line -> toXml(line, calculationService.calculateLineAmounts(line)))
                .toList();
    }

    private XmlInvoiceBatch.LineXml toXml(InvoiceLine line, InvoiceLineAmounts amounts) {
        return new XmlInvoiceBatch.LineXml(
                line.lineNumber(),
                optional(line.itemCode()),
                line.description(),
                line.quantity(),
                line.unitCode(),
                line.unitPrice(),
                line.taxRate(),
                amounts.lineNetAmount(),
                amounts.lineTaxAmount()
        );
    }

    private XmlInvoiceBatch.TotalsXml toXml(InvoiceTotals totals) {
        return new XmlInvoiceBatch.TotalsXml(
                totals.taxExclusiveAmount(),
                totals.taxAmount(),
                totals.payableAmount()
        );
    }

    private String optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
