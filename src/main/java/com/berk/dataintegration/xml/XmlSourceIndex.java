package com.berk.dataintegration.xml;

import com.berk.dataintegration.validation.ValidationError;
import com.berk.dataintegration.xml.dto.XmlInvoiceBatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class XmlSourceIndex {
    private final Map<String, XmlSourceLocation> byRecordAndField = new HashMap<>();
    private final Map<String, XmlSourceLocation> byRecord = new HashMap<>();

    static XmlSourceIndex from(XmlInvoiceBatch xmlBatch, String sourceFile) {
        XmlSourceIndex index = new XmlSourceIndex();
        index.indexParties(xmlBatch.getParties().getParties(), sourceFile);
        index.indexPaymentAccounts(xmlBatch.getPaymentAccounts().getPaymentAccounts(), sourceFile);
        index.indexInvoices(xmlBatch.getInvoices().getInvoices(), sourceFile);
        return index;
    }

    ValidationError enrich(ValidationError error) {
        if (error.sourceFile() != null || error.recordId() == null) {
            return error;
        }
        XmlSourceLocation location = byRecordAndField.get(key(error.recordId(), error.field()));
        if (location == null) {
            location = byRecord.get(error.recordId());
        }
        if (location == null) {
            return error;
        }
        return new ValidationError(
                error.code(),
                error.category(),
                error.message(),
                error.field(),
                error.recordId(),
                location.sourceFile(),
                location.xmlPath(),
                null,
                null,
                location.value()
        );
    }

    private void indexParties(List<XmlInvoiceBatch.PartyXml> parties, String sourceFile) {
        for (int index = 0; index < parties.size(); index++) {
            XmlInvoiceBatch.PartyXml party = parties.get(index);
            String recordId = party.getPartyId();
            String basePath = "/InvoiceBatch/Parties/Party[" + (index + 1) + "]";
            put(recordId, "partyId", sourceFile, basePath + "/PartyId", party.getPartyId());
            put(recordId, "role", sourceFile, basePath + "/Role", party.getRole());
            put(recordId, "name", sourceFile, basePath + "/Name", party.getName());
            put(recordId, "taxNumber", sourceFile, basePath + "/TaxNumber", party.getTaxNumber());
            put(recordId, "email", sourceFile, basePath + "/Email", party.getEmail());
            put(recordId, "phone", sourceFile, basePath + "/Phone", party.getPhone());
            XmlInvoiceBatch.AddressXml address = party.getAddress();
            put(recordId, "address.street", sourceFile, basePath + "/Address/Street", address.getStreet());
            put(recordId, "address.city", sourceFile, basePath + "/Address/City", address.getCity());
            put(recordId, "address.postalCode", sourceFile, basePath + "/Address/PostalCode", address.getPostalCode());
            put(recordId, "address.countryCode", sourceFile, basePath + "/Address/CountryCode", address.getCountryCode());
        }
    }

    private void indexPaymentAccounts(List<XmlInvoiceBatch.PaymentAccountXml> paymentAccounts, String sourceFile) {
        for (int index = 0; index < paymentAccounts.size(); index++) {
            XmlInvoiceBatch.PaymentAccountXml account = paymentAccounts.get(index);
            String recordId = account.getPaymentAccountId();
            String basePath = "/InvoiceBatch/PaymentAccounts/PaymentAccount[" + (index + 1) + "]";
            put(recordId, "paymentAccountId", sourceFile, basePath + "/PaymentAccountId", account.getPaymentAccountId());
            put(recordId, "accountHolderName", sourceFile, basePath + "/AccountHolderName", account.getAccountHolderName());
            put(recordId, "bankName", sourceFile, basePath + "/BankName", account.getBankName());
            put(recordId, "iban", sourceFile, basePath + "/IBAN", account.getIban());
            put(recordId, "swiftCode", sourceFile, basePath + "/SwiftCode", account.getSwiftCode());
            put(recordId, "currencyCode", sourceFile, basePath + "/CurrencyCode", account.getCurrencyCode());
        }
    }

    private void indexInvoices(List<XmlInvoiceBatch.InvoiceXml> invoices, String sourceFile) {
        for (int invoiceIndex = 0; invoiceIndex < invoices.size(); invoiceIndex++) {
            XmlInvoiceBatch.InvoiceXml invoice = invoices.get(invoiceIndex);
            String recordId = invoice.getInvoiceNumber();
            String basePath = "/InvoiceBatch/Invoices/Invoice[" + (invoiceIndex + 1) + "]";
            put(recordId, "invoiceNumber", sourceFile, basePath + "/InvoiceNumber", invoice.getInvoiceNumber());
            put(recordId, "issueDate", sourceFile, basePath + "/IssueDate", value(invoice.getIssueDate()));
            put(recordId, "dueDate", sourceFile, basePath + "/DueDate", value(invoice.getDueDate()));
            put(recordId, "currencyCode", sourceFile, basePath + "/CurrencyCode", invoice.getCurrencyCode());
            put(recordId, "supplierId", sourceFile, basePath + "/SupplierPartyId", invoice.getSupplierPartyId());
            put(recordId, "customerId", sourceFile, basePath + "/CustomerPartyId", invoice.getCustomerPartyId());
            put(recordId, "paymentAccountId", sourceFile, basePath + "/PaymentAccountId", invoice.getPaymentAccountId());
            put(recordId, "note", sourceFile, basePath + "/Note", invoice.getNote());
            put(recordId, "lines", sourceFile, basePath + "/Lines", null);
            indexLines(invoice.getLines().getLines(), recordId, basePath, sourceFile);
        }
    }

    private void indexLines(
            List<XmlInvoiceBatch.LineXml> lines,
            String invoiceNumber,
            String invoicePath,
            String sourceFile
    ) {
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            XmlInvoiceBatch.LineXml line = lines.get(lineIndex);
            String recordId = invoiceNumber + "#" + line.getLineNumber();
            String basePath = invoicePath + "/Lines/Line[" + (lineIndex + 1) + "]";
            put(recordId, "lineNumber", sourceFile, basePath + "/LineNumber", Integer.toString(line.getLineNumber()));
            put(recordId, "itemCode", sourceFile, basePath + "/ItemCode", line.getItemCode());
            put(recordId, "description", sourceFile, basePath + "/Description", line.getDescription());
            put(recordId, "quantity", sourceFile, basePath + "/Quantity", value(line.getQuantity()));
            put(recordId, "unitCode", sourceFile, basePath + "/UnitCode", line.getUnitCode());
            put(recordId, "unitPrice", sourceFile, basePath + "/UnitPrice", value(line.getUnitPrice()));
            put(recordId, "taxRate", sourceFile, basePath + "/TaxRate", value(line.getTaxRate()));
        }
    }

    private void put(String recordId, String field, String sourceFile, String xmlPath, String value) {
        if (recordId == null || recordId.isBlank()) {
            return;
        }
        XmlSourceLocation location = new XmlSourceLocation(sourceFile, xmlPath, field, value);
        byRecord.put(recordId, location);
        if (field != null && !field.isBlank()) {
            byRecordAndField.put(key(recordId, field), location);
        }
    }

    private String key(String recordId, String field) {
        return recordId + "\u0000" + field;
    }

    private String value(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private String value(LocalDate value) {
        return value == null ? null : value.toString();
    }
}
