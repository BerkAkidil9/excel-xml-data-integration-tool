package com.berk.dataintegration.xml;

import com.berk.dataintegration.domain.Address;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.domain.Party;
import com.berk.dataintegration.domain.PartyRole;
import com.berk.dataintegration.domain.PaymentAccount;
import com.berk.dataintegration.xml.dto.XmlInvoiceBatch;

import java.util.List;

public final class XmlInvoiceBatchDomainMapper {
    public InvoiceBatch toDomain(XmlInvoiceBatch xmlBatch) {
        return new InvoiceBatch(
                xmlBatch.getParties().getParties().stream().map(this::toDomain).toList(),
                xmlBatch.getPaymentAccounts().getPaymentAccounts().stream().map(this::toDomain).toList(),
                xmlBatch.getInvoices().getInvoices().stream().map(this::toDomain).toList()
        );
    }

    private Party toDomain(XmlInvoiceBatch.PartyXml party) {
        return new Party(
                party.getPartyId(),
                PartyRole.valueOf(party.getRole()),
                party.getName(),
                party.getTaxNumber(),
                party.getEmail(),
                party.getPhone(),
                toDomain(party.getAddress())
        );
    }

    private Address toDomain(XmlInvoiceBatch.AddressXml address) {
        return new Address(
                address.getStreet(),
                address.getCity(),
                address.getPostalCode(),
                address.getCountryCode()
        );
    }

    private PaymentAccount toDomain(XmlInvoiceBatch.PaymentAccountXml paymentAccount) {
        return new PaymentAccount(
                paymentAccount.getPaymentAccountId(),
                paymentAccount.getAccountHolderName(),
                paymentAccount.getBankName(),
                paymentAccount.getIban(),
                paymentAccount.getSwiftCode(),
                paymentAccount.getCurrencyCode()
        );
    }

    private Invoice toDomain(XmlInvoiceBatch.InvoiceXml invoice) {
        return new Invoice(
                invoice.getInvoiceNumber(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getCurrencyCode(),
                invoice.getSupplierPartyId(),
                invoice.getCustomerPartyId(),
                invoice.getPaymentAccountId(),
                invoice.getNote(),
                toDomainLines(invoice.getLines())
        );
    }

    private List<InvoiceLine> toDomainLines(XmlInvoiceBatch.LinesXml lines) {
        return lines.getLines().stream().map(this::toDomain).toList();
    }

    private InvoiceLine toDomain(XmlInvoiceBatch.LineXml line) {
        return new InvoiceLine(
                line.getLineNumber(),
                line.getItemCode(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnitCode(),
                line.getUnitPrice(),
                line.getTaxRate()
        );
    }
}
