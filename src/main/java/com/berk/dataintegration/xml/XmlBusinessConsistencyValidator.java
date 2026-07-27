package com.berk.dataintegration.xml;

import com.berk.dataintegration.calculation.MoneyCalculationService;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLineAmounts;
import com.berk.dataintegration.domain.InvoiceTotals;
import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import com.berk.dataintegration.xml.dto.XmlInvoiceBatch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class XmlBusinessConsistencyValidator {
    private final MoneyCalculationService calculationService;

    public XmlBusinessConsistencyValidator() {
        this(new MoneyCalculationService());
    }

    XmlBusinessConsistencyValidator(MoneyCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    public List<ValidationError> validate(XmlInvoiceBatch xmlBatch, InvoiceBatch domainBatch, String sourceFile) {
        List<ValidationError> errors = new ArrayList<>();
        List<XmlInvoiceBatch.InvoiceXml> xmlInvoices = xmlBatch.getInvoices().getInvoices();
        for (int invoiceIndex = 0; invoiceIndex < domainBatch.invoices().size(); invoiceIndex++) {
            Invoice invoice = domainBatch.invoices().get(invoiceIndex);
            XmlInvoiceBatch.InvoiceXml xmlInvoice = xmlInvoices.get(invoiceIndex);
            validateLines(xmlInvoice, invoice, invoiceIndex, sourceFile, errors);
            validateTotals(xmlInvoice, invoice, invoiceIndex, sourceFile, errors);
        }
        return errors;
    }

    private void validateLines(
            XmlInvoiceBatch.InvoiceXml xmlInvoice,
            Invoice invoice,
            int invoiceIndex,
            String sourceFile,
            List<ValidationError> errors
    ) {
        List<XmlInvoiceBatch.LineXml> xmlLines = xmlInvoice.getLines().getLines();
        for (int lineIndex = 0; lineIndex < invoice.lines().size(); lineIndex++) {
            XmlInvoiceBatch.LineXml xmlLine = xmlLines.get(lineIndex);
            InvoiceLineAmounts expected = calculationService.calculateLineAmounts(invoice.lines().get(lineIndex));
            String recordId = lineRecordId(invoice.invoiceNumber(), xmlLine.getLineNumber());
            if (!sameMoney(expected.lineNetAmount(), xmlLine.getLineNetAmount())) {
                errors.add(totalError(
                        ValidationCode.XML_LINE_TOTAL_MISMATCH,
                        "XML line net amount does not match recalculation.",
                        "lineNetAmount",
                        recordId,
                        sourceFile,
                        linePath(invoiceIndex, lineIndex, "LineNetAmount"),
                        xmlLine.getLineNetAmount()
                ));
            }
            if (!sameMoney(expected.lineTaxAmount(), xmlLine.getLineTaxAmount())) {
                errors.add(totalError(
                        ValidationCode.XML_LINE_TOTAL_MISMATCH,
                        "XML line tax amount does not match recalculation.",
                        "lineTaxAmount",
                        recordId,
                        sourceFile,
                        linePath(invoiceIndex, lineIndex, "LineTaxAmount"),
                        xmlLine.getLineTaxAmount()
                ));
            }
        }
    }

    private void validateTotals(
            XmlInvoiceBatch.InvoiceXml xmlInvoice,
            Invoice invoice,
            int invoiceIndex,
            String sourceFile,
            List<ValidationError> errors
    ) {
        InvoiceTotals expected = calculationService.calculateInvoiceTotals(invoice);
        XmlInvoiceBatch.TotalsXml actual = xmlInvoice.getTotals();
        String recordId = invoice.invoiceNumber();
        if (!sameMoney(expected.taxExclusiveAmount(), actual.getTaxExclusiveAmount())) {
            errors.add(totalError(
                    ValidationCode.XML_INVOICE_TOTAL_MISMATCH,
                    "XML tax exclusive amount does not match recalculation.",
                    "taxExclusiveAmount",
                    recordId,
                    sourceFile,
                    totalPath(invoiceIndex, "TaxExclusiveAmount"),
                    actual.getTaxExclusiveAmount()
            ));
        }
        if (!sameMoney(expected.taxAmount(), actual.getTaxAmount())) {
            errors.add(totalError(
                    ValidationCode.XML_INVOICE_TOTAL_MISMATCH,
                    "XML tax amount does not match recalculation.",
                    "taxAmount",
                    recordId,
                    sourceFile,
                    totalPath(invoiceIndex, "TaxAmount"),
                    actual.getTaxAmount()
            ));
        }
        if (!sameMoney(expected.payableAmount(), actual.getPayableAmount())) {
            errors.add(totalError(
                    ValidationCode.XML_INVOICE_TOTAL_MISMATCH,
                    "XML payable amount does not match recalculation.",
                    "payableAmount",
                    recordId,
                    sourceFile,
                    totalPath(invoiceIndex, "PayableAmount"),
                    actual.getPayableAmount()
            ));
        }
    }

    private ValidationError totalError(
            ValidationCode code,
            String message,
            String field,
            String recordId,
            String sourceFile,
            String xmlPath,
            BigDecimal value
    ) {
        return XmlValidationErrors.xmlError(
                code,
                ValidationCategory.BUSINESS_RULE,
                message,
                field,
                recordId,
                sourceFile,
                xmlPath,
                null,
                null,
                value == null ? null : value.toPlainString()
        );
    }

    private boolean sameMoney(BigDecimal expected, BigDecimal actual) {
        return expected != null && actual != null && expected.compareTo(actual) == 0;
    }

    private String lineRecordId(String invoiceNumber, int lineNumber) {
        return invoiceNumber + "#" + lineNumber;
    }

    private String linePath(int invoiceIndex, int lineIndex, String field) {
        return "/InvoiceBatch/Invoices/Invoice[" + (invoiceIndex + 1) + "]/Lines/Line[" + (lineIndex + 1) + "]/" + field;
    }

    private String totalPath(int invoiceIndex, String field) {
        return "/InvoiceBatch/Invoices/Invoice[" + (invoiceIndex + 1) + "]/Totals/" + field;
    }
}
