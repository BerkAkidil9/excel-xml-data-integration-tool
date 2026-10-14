# Data Contract — invoice-data-v1

## 1. Contract identity

- Contract name: `invoice-data-v1`
- XML namespace: `urn:berk:excel-xml-integration:invoice:v1`
- XML root: `InvoiceBatch`
- Excel contract: the four sheet/header definitions documented in [excel-format.md](excel-format.md); no workbook version field is read or validated
- XML version attribute: `1.0`

The Excel and XML forms represent the same logical data. This document describes the current implementation, reviewed on 2026-10-14. Field and cross-record validation is implemented in [InvoiceBatchValidator.java](../../src/main/java/com/berk/dataintegration/validation/InvoiceBatchValidator.java); XML input also has to satisfy the bundled [XSD](../../src/main/resources/schema/invoice-data-v1.xsd) before domain normalization.

Domain text is trimmed. Blank optional text becomes `null`; uppercase code fields are validated rather than automatically converted to uppercase.

## 2. Domain relationships

```text
InvoiceBatch
├── Party[]
├── PaymentAccount[]
└── Invoice[]
    └── InvoiceLine[]
```

Each invoice references:

- one supplier party
- one customer party
- one payment account
- one or more invoice lines

## 3. Party

| Field | Java type | Required | Rule |
|---|---|---:|---|
| partyId | String | Yes | Unique, trimmed, 1–40 chars |
| role | PartyRole | Yes | `SUPPLIER` or `CUSTOMER` |
| name | String | Yes | 1–200 chars |
| taxNumber | String | No | 1–30 chars when present; no national algorithm in v1 |
| email | String | No | Max 320 chars; basic email shape only when present |
| phone | String | No | 1–30 chars when present |
| street | String | No | Max 250 chars |
| city | String | Yes | 1–100 chars |
| postalCode | String | No | Max 20 chars |
| countryCode | String | Yes | Two uppercase letters |

A party ID is unique across the entire batch.

## 4. Payment account

| Field | Java type | Required | Rule |
|---|---|---:|---|
| paymentAccountId | String | Yes | Unique, 1–40 chars |
| accountHolderName | String | Yes | 1–200 chars |
| bankName | String | Yes | 1–200 chars |
| iban | String | Yes | For Excel/XML interchange, use 5–34 uppercase alphanumeric chars without spaces; no checksum algorithm in v1 |
| swiftCode | String | No | 8 or 11 uppercase alphanumeric chars when present |
| currencyCode | String | Yes | Three uppercase letters |

A payment account may be reused by multiple invoices.

The Java field validator allows literal spaces when checking the IBAN pattern, but limits the trimmed original value to 34 characters and does not remove internal spaces from the stored value. The XML schema requires an unspaced IBAN, so a spaced Excel value can pass field validation and still fail XML generation. Use an unspaced value for both formats.

## 5. Invoice

| Field | Java type | Required | Rule |
|---|---|---:|---|
| invoiceNumber | String | Yes | Unique, 1–50 chars |
| issueDate | LocalDate | Yes | ISO `yyyy-MM-dd` |
| dueDate | LocalDate | No | Must be on or after issue date |
| currencyCode | String | Yes | Three uppercase letters |
| supplierId | String | Yes | Existing party with role `SUPPLIER` |
| customerId | String | Yes | Existing party with role `CUSTOMER` |
| paymentAccountId | String | Yes | Existing payment account |
| note | String | No | Max 1000 chars |
| lines | List<InvoiceLine> | Yes | At least one line |

The invoice currency must equal the referenced payment-account currency in v1.

## 6. Invoice line

| Field | Java type | Required | Rule |
|---|---|---:|---|
| lineNumber | int | Yes | Positive and unique within the invoice |
| itemCode | String | No | Max 50 chars |
| description | String | Yes | 1–500 chars |
| quantity | BigDecimal | Yes | Greater than zero, max 4 decimal places |
| unitCode | String | Yes | 1–8 uppercase letters/digits; example `C62` |
| unitPrice | BigDecimal | Yes | Zero or greater, max 4 decimal places |
| taxRate | BigDecimal | Yes | Between 0 and 100 inclusive, max 2 decimal places |

## 7. Calculated values

Calculated values are not manually supplied in the Excel input sheets.

For every line:

```text
rawNetAmount = quantity × unitPrice
rawTaxAmount = rawNetAmount × taxRate ÷ 100
lineNetAmount = roundHalfUp(rawNetAmount, 2)
lineTaxAmount = roundHalfUp(rawTaxAmount, 2)
```

For every invoice:

```text
taxExclusiveAmount = sum(lineNetAmount)
taxAmount = sum(lineTaxAmount)
payableAmount = taxExclusiveAmount + taxAmount
```

Rounding rules:

- Round each line net and line tax amount to scale 2.
- Calculate line tax from the unrounded net amount, then round net and tax independently.
- Use `RoundingMode.HALF_UP`.
- Sum rounded line values.
- Store and output monetary values with two decimal places.

XML contains calculated values. On XML import, the application recalculates them and rejects mismatches.

Decimal-scale validation ignores trailing zeros: for example, `1.23000` has an effective scale of 2. Money calculations are implemented in [MoneyCalculationService.java](../../src/main/java/com/berk/dataintegration/calculation/MoneyCalculationService.java).

## 8. Cross-record rules

- IDs are compared after trimming.
- IDs are case-sensitive.
- Duplicate IDs are errors.
- Invoice numbers are unique across the batch; line numbers are positive and unique within each invoice.
- Missing references are errors.
- Supplier and customer cannot reference the same party ID in one invoice.
- A referenced supplier must have role `SUPPLIER`.
- A referenced customer must have role `CUSTOMER`.
- Every invoice must have at least one line.
- Every invoice-line `invoiceNumber` in Excel must reference an existing invoice.
- Orphan parties and payment accounts are allowed; they may exist without being referenced.
- The due date cannot precede the issue date.
- Invoice and payment-account currencies must match.

## 9. Excel-to-XML mapping

All 32 sheet/column positions are listed below. Abbreviated XML paths in the payment-account, invoice, and line rows continue within the respective `PaymentAccount`, `Invoice`, and `Invoice/Lines/Line` elements.

| Excel sheet.column | Domain field | XML path |
|---|---|---|
| Parties.party_id | Party.partyId | InvoiceBatch/Parties/Party/PartyId |
| Parties.party_role | Party.role | InvoiceBatch/Parties/Party/Role |
| Parties.name | Party.name | InvoiceBatch/Parties/Party/Name |
| Parties.tax_number | Party.taxNumber | InvoiceBatch/Parties/Party/TaxNumber |
| Parties.email | Party.email | InvoiceBatch/Parties/Party/Email |
| Parties.phone | Party.phone | InvoiceBatch/Parties/Party/Phone |
| Parties.street | Address.street | InvoiceBatch/Parties/Party/Address/Street |
| Parties.city | Address.city | InvoiceBatch/Parties/Party/Address/City |
| Parties.postal_code | Address.postalCode | InvoiceBatch/Parties/Party/Address/PostalCode |
| Parties.country_code | Address.countryCode | InvoiceBatch/Parties/Party/Address/CountryCode |
| PaymentAccounts.payment_account_id | PaymentAccount.paymentAccountId | InvoiceBatch/PaymentAccounts/PaymentAccount/PaymentAccountId |
| PaymentAccounts.account_holder_name | PaymentAccount.accountHolderName | .../AccountHolderName |
| PaymentAccounts.bank_name | PaymentAccount.bankName | .../BankName |
| PaymentAccounts.iban | PaymentAccount.iban | .../IBAN |
| PaymentAccounts.swift_code | PaymentAccount.swiftCode | .../SwiftCode |
| PaymentAccounts.currency_code | PaymentAccount.currencyCode | .../CurrencyCode |
| Invoices.invoice_number | Invoice.invoiceNumber | InvoiceBatch/Invoices/Invoice/InvoiceNumber |
| Invoices.issue_date | Invoice.issueDate | .../IssueDate |
| Invoices.due_date | Invoice.dueDate | .../DueDate |
| Invoices.currency_code | Invoice.currencyCode | .../CurrencyCode |
| Invoices.supplier_id | Invoice.supplierId | .../SupplierPartyId |
| Invoices.customer_id | Invoice.customerId | .../CustomerPartyId |
| Invoices.payment_account_id | Invoice.paymentAccountId | .../PaymentAccountId |
| Invoices.note | Invoice.note | .../Note |
| InvoiceLines.invoice_number | Associates InvoiceLine with its parent Invoice by Invoice.invoiceNumber | Selects InvoiceBatch/Invoices/Invoice by InvoiceNumber; not repeated inside Line |
| InvoiceLines.line_number | InvoiceLine.lineNumber | .../Lines/Line/LineNumber |
| InvoiceLines.item_code | InvoiceLine.itemCode | .../Lines/Line/ItemCode |
| InvoiceLines.description | InvoiceLine.description | .../Lines/Line/Description |
| InvoiceLines.quantity | InvoiceLine.quantity | .../Lines/Line/Quantity |
| InvoiceLines.unit_code | InvoiceLine.unitCode | .../Lines/Line/UnitCode |
| InvoiceLines.unit_price | InvoiceLine.unitPrice | .../Lines/Line/UnitPrice |
| InvoiceLines.tax_rate | InvoiceLine.taxRate | .../Lines/Line/TaxRate |

XML also contains `LineNetAmount`, `LineTaxAmount`, `TaxExclusiveAmount`, `TaxAmount`, and `PayableAmount`. These are calculated values, not additional Excel input columns.

XML requires at least one `Party`, `PaymentAccount`, and `Invoice`, with at least one `Line` per invoice. The blank Excel template supplies headers only and must be populated before it can produce a schema-valid XML document.

## 10. Not supported in v1

- legally compliant e-invoice generation
- UBL/UBL-TR/Peppol schemas
- discounts and charges
- multiple taxes on one line
- withholding or exemptions
- shipping data
- attachments
- digital signatures
- exchange rates
