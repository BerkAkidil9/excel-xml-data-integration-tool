# Data Contract — invoice-data-v1

## 1. Contract identity

- Contract name: `invoice-data-v1`
- XML namespace: `urn:berk:excel-xml-integration:invoice:v1`
- XML root: `InvoiceBatch`
- Excel workbook version: `1.0`
- XML version attribute: `1.0`

The Excel and XML forms represent the same logical data.

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
| email | String | No | Basic email shape only when present |
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
| iban | String | Yes | 5–34 uppercase alphanumeric chars after spaces are removed; no checksum algorithm in v1 |
| swiftCode | String | No | 8 or 11 uppercase alphanumeric chars when present |
| currencyCode | String | Yes | Three uppercase letters |

A payment account may be reused by multiple invoices.

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
lineNetAmount = quantity × unitPrice
lineTaxAmount = lineNetAmount × taxRate ÷ 100
```

For every invoice:

```text
taxExclusiveAmount = sum(lineNetAmount)
taxAmount = sum(lineTaxAmount)
payableAmount = taxExclusiveAmount + taxAmount
```

Rounding rules:

- Round each line net and line tax amount to scale 2.
- Use `RoundingMode.HALF_UP`.
- Sum rounded line values.
- Store and output monetary values with two decimal places.

XML contains calculated values. On XML import, the application recalculates them and rejects mismatches.

## 8. Cross-record rules

- IDs are compared after trimming.
- IDs are case-sensitive.
- Duplicate IDs are errors.
- Missing references are errors.
- Supplier and customer cannot reference the same party ID in one invoice.
- A referenced supplier must have role `SUPPLIER`.
- A referenced customer must have role `CUSTOMER`.
- Every invoice must have at least one line.
- Every invoice-line `invoiceNumber` in Excel must reference an existing invoice.
- Orphan parties and payment accounts are allowed; they may exist without being referenced.

## 9. Excel-to-XML mapping

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
| InvoiceLines.line_number | InvoiceLine.lineNumber | .../Lines/Line/LineNumber |
| InvoiceLines.item_code | InvoiceLine.itemCode | .../Lines/Line/ItemCode |
| InvoiceLines.description | InvoiceLine.description | .../Lines/Line/Description |
| InvoiceLines.quantity | InvoiceLine.quantity | .../Lines/Line/Quantity |
| InvoiceLines.unit_code | InvoiceLine.unitCode | .../Lines/Line/UnitCode |
| InvoiceLines.unit_price | InvoiceLine.unitPrice | .../Lines/Line/UnitPrice |
| InvoiceLines.tax_rate | InvoiceLine.taxRate | .../Lines/Line/TaxRate |

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
