package com.berk.dataintegration.xml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "InvoiceBatch")
@XmlType(name = "", propOrder = {"parties", "paymentAccounts", "invoices"})
public final class XmlInvoiceBatch {
    @XmlElement(name = "Parties", required = true)
    private PartiesXml parties;

    @XmlElement(name = "PaymentAccounts", required = true)
    private PaymentAccountsXml paymentAccounts;

    @XmlElement(name = "Invoices", required = true)
    private InvoicesXml invoices;

    @XmlAttribute(name = "version", required = true)
    private String version = "1.0";

    public XmlInvoiceBatch() {
    }

    public XmlInvoiceBatch(PartiesXml parties, PaymentAccountsXml paymentAccounts, InvoicesXml invoices) {
        this.parties = parties;
        this.paymentAccounts = paymentAccounts;
        this.invoices = invoices;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"parties"})
    public static final class PartiesXml {
        @XmlElement(name = "Party", required = true)
        private List<PartyXml> parties = new ArrayList<>();

        public PartiesXml() {
        }

        public PartiesXml(List<PartyXml> parties) {
            this.parties = List.copyOf(parties);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"paymentAccounts"})
    public static final class PaymentAccountsXml {
        @XmlElement(name = "PaymentAccount", required = true)
        private List<PaymentAccountXml> paymentAccounts = new ArrayList<>();

        public PaymentAccountsXml() {
        }

        public PaymentAccountsXml(List<PaymentAccountXml> paymentAccounts) {
            this.paymentAccounts = List.copyOf(paymentAccounts);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"invoices"})
    public static final class InvoicesXml {
        @XmlElement(name = "Invoice", required = true)
        private List<InvoiceXml> invoices = new ArrayList<>();

        public InvoicesXml() {
        }

        public InvoicesXml(List<InvoiceXml> invoices) {
            this.invoices = List.copyOf(invoices);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "PartyType", propOrder = {
            "partyId",
            "role",
            "name",
            "taxNumber",
            "email",
            "phone",
            "address"
    })
    public static final class PartyXml {
        @XmlElement(name = "PartyId", required = true)
        private String partyId;

        @XmlElement(name = "Role", required = true)
        private String role;

        @XmlElement(name = "Name", required = true)
        private String name;

        @XmlElement(name = "TaxNumber")
        private String taxNumber;

        @XmlElement(name = "Email")
        private String email;

        @XmlElement(name = "Phone")
        private String phone;

        @XmlElement(name = "Address", required = true)
        private AddressXml address;

        public PartyXml() {
        }

        public PartyXml(
                String partyId,
                String role,
                String name,
                String taxNumber,
                String email,
                String phone,
                AddressXml address
        ) {
            this.partyId = partyId;
            this.role = role;
            this.name = name;
            this.taxNumber = taxNumber;
            this.email = email;
            this.phone = phone;
            this.address = address;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "AddressType", propOrder = {"street", "city", "postalCode", "countryCode"})
    public static final class AddressXml {
        @XmlElement(name = "Street")
        private String street;

        @XmlElement(name = "City", required = true)
        private String city;

        @XmlElement(name = "PostalCode")
        private String postalCode;

        @XmlElement(name = "CountryCode", required = true)
        private String countryCode;

        public AddressXml() {
        }

        public AddressXml(String street, String city, String postalCode, String countryCode) {
            this.street = street;
            this.city = city;
            this.postalCode = postalCode;
            this.countryCode = countryCode;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "PaymentAccountType", propOrder = {
            "paymentAccountId",
            "accountHolderName",
            "bankName",
            "iban",
            "swiftCode",
            "currencyCode"
    })
    public static final class PaymentAccountXml {
        @XmlElement(name = "PaymentAccountId", required = true)
        private String paymentAccountId;

        @XmlElement(name = "AccountHolderName", required = true)
        private String accountHolderName;

        @XmlElement(name = "BankName", required = true)
        private String bankName;

        @XmlElement(name = "IBAN", required = true)
        private String iban;

        @XmlElement(name = "SwiftCode")
        private String swiftCode;

        @XmlElement(name = "CurrencyCode", required = true)
        private String currencyCode;

        public PaymentAccountXml() {
        }

        public PaymentAccountXml(
                String paymentAccountId,
                String accountHolderName,
                String bankName,
                String iban,
                String swiftCode,
                String currencyCode
        ) {
            this.paymentAccountId = paymentAccountId;
            this.accountHolderName = accountHolderName;
            this.bankName = bankName;
            this.iban = iban;
            this.swiftCode = swiftCode;
            this.currencyCode = currencyCode;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "InvoiceType", propOrder = {
            "invoiceNumber",
            "issueDate",
            "dueDate",
            "currencyCode",
            "supplierPartyId",
            "customerPartyId",
            "paymentAccountId",
            "note",
            "lines",
            "totals"
    })
    public static final class InvoiceXml {
        @XmlElement(name = "InvoiceNumber", required = true)
        private String invoiceNumber;

        @XmlElement(name = "IssueDate", required = true)
        @XmlJavaTypeAdapter(LocalDateAdapter.class)
        private LocalDate issueDate;

        @XmlElement(name = "DueDate")
        @XmlJavaTypeAdapter(LocalDateAdapter.class)
        private LocalDate dueDate;

        @XmlElement(name = "CurrencyCode", required = true)
        private String currencyCode;

        @XmlElement(name = "SupplierPartyId", required = true)
        private String supplierPartyId;

        @XmlElement(name = "CustomerPartyId", required = true)
        private String customerPartyId;

        @XmlElement(name = "PaymentAccountId", required = true)
        private String paymentAccountId;

        @XmlElement(name = "Note")
        private String note;

        @XmlElement(name = "Lines", required = true)
        private LinesXml lines;

        @XmlElement(name = "Totals", required = true)
        private TotalsXml totals;

        public InvoiceXml() {
        }

        public InvoiceXml(
                String invoiceNumber,
                LocalDate issueDate,
                LocalDate dueDate,
                String currencyCode,
                String supplierPartyId,
                String customerPartyId,
                String paymentAccountId,
                String note,
                LinesXml lines,
                TotalsXml totals
        ) {
            this.invoiceNumber = invoiceNumber;
            this.issueDate = issueDate;
            this.dueDate = dueDate;
            this.currencyCode = currencyCode;
            this.supplierPartyId = supplierPartyId;
            this.customerPartyId = customerPartyId;
            this.paymentAccountId = paymentAccountId;
            this.note = note;
            this.lines = lines;
            this.totals = totals;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"lines"})
    public static final class LinesXml {
        @XmlElement(name = "Line", required = true)
        private List<LineXml> lines = new ArrayList<>();

        public LinesXml() {
        }

        public LinesXml(List<LineXml> lines) {
            this.lines = List.copyOf(lines);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "InvoiceLineType", propOrder = {
            "lineNumber",
            "itemCode",
            "description",
            "quantity",
            "unitCode",
            "unitPrice",
            "taxRate",
            "lineNetAmount",
            "lineTaxAmount"
    })
    public static final class LineXml {
        @XmlElement(name = "LineNumber", required = true)
        private int lineNumber;

        @XmlElement(name = "ItemCode")
        private String itemCode;

        @XmlElement(name = "Description", required = true)
        private String description;

        @XmlElement(name = "Quantity", required = true)
        private BigDecimal quantity;

        @XmlElement(name = "UnitCode", required = true)
        private String unitCode;

        @XmlElement(name = "UnitPrice", required = true)
        private BigDecimal unitPrice;

        @XmlElement(name = "TaxRate", required = true)
        private BigDecimal taxRate;

        @XmlElement(name = "LineNetAmount", required = true)
        private BigDecimal lineNetAmount;

        @XmlElement(name = "LineTaxAmount", required = true)
        private BigDecimal lineTaxAmount;

        public LineXml() {
        }

        public LineXml(
                int lineNumber,
                String itemCode,
                String description,
                BigDecimal quantity,
                String unitCode,
                BigDecimal unitPrice,
                BigDecimal taxRate,
                BigDecimal lineNetAmount,
                BigDecimal lineTaxAmount
        ) {
            this.lineNumber = lineNumber;
            this.itemCode = itemCode;
            this.description = description;
            this.quantity = quantity;
            this.unitCode = unitCode;
            this.unitPrice = unitPrice;
            this.taxRate = taxRate;
            this.lineNetAmount = lineNetAmount;
            this.lineTaxAmount = lineTaxAmount;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "InvoiceTotalsType", propOrder = {"taxExclusiveAmount", "taxAmount", "payableAmount"})
    public static final class TotalsXml {
        @XmlElement(name = "TaxExclusiveAmount", required = true)
        private BigDecimal taxExclusiveAmount;

        @XmlElement(name = "TaxAmount", required = true)
        private BigDecimal taxAmount;

        @XmlElement(name = "PayableAmount", required = true)
        private BigDecimal payableAmount;

        public TotalsXml() {
        }

        public TotalsXml(BigDecimal taxExclusiveAmount, BigDecimal taxAmount, BigDecimal payableAmount) {
            this.taxExclusiveAmount = taxExclusiveAmount;
            this.taxAmount = taxAmount;
            this.payableAmount = payableAmount;
        }
    }
}
