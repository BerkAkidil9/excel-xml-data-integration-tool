# Excel Format — invoice-data-v1

The current sheet and column definitions are implemented in [ExcelSheetDefinition.java](../../src/main/java/com/berk/dataintegration/excel/ExcelSheetDefinition.java). This document was checked against the reader and writer on 2026-10-14.

## General workbook rules

- File extension: `.xlsx`
- Required sheets: `Parties`, `PaymentAccounts`, `Invoices`, `InvoiceLines`
- Optional informational sheets: `README`, `ReferenceLists`
- Required sheet names are case-sensitive.
- Sheet tab order does not matter; column order within each required sheet must match the tables below.
- Header names are exact, lowercase `snake_case`, and case-sensitive after trimming surrounding whitespace.
- Header row is row 1.
- Data starts at row 2.
- Extra sheets are ignored by the reader.
- Nonblank header cells beyond the defined columns are contract errors. Blank trailing headers are ignored; data in columns outside the contract is not imported.
- A fully empty row is ignored.
- A partially populated row is validated and may fail; it is never silently skipped.
- Formula cells in contract data columns are always rejected with `UNSUPPORTED_FORMULA`, regardless of whether a cached result is available. The application does not evaluate formulas.

Across the required sheets there are 32 column positions: `Parties` 10, `PaymentAccounts` 6, `Invoices` 8, and `InvoiceLines` 8. Headers are required even for optional data fields. Text fields, including IDs, tax numbers, phone numbers, and postal codes, must use text cells; numeric cells are not automatically converted into those fields.

## Date input

The reader accepts either:

1. a real Excel date cell, or
2. ISO text `yyyy-MM-dd`.

The writer produces real Excel date cells formatted as `yyyy-mm-dd`.

## Decimal input

- Numeric Excel cells are preferred.
- Decimal text using `.` is accepted, with optional leading `+` or `-`; for example `1250.00` or `-1.5`. Grouping separators and exponent notation are rejected in text cells.
- Locale-specific text such as `1.234,56` is rejected with `INVALID_DECIMAL_FORMAT`.
- The writer produces numeric cells.

Numeric `line_number` cells must contain a whole number within Java's `int` range. Integer text is also accepted. Domain validation additionally requires a positive line number and enforces the quantity, price, and tax-rate limits in the [shared contract](data-contract.md).

## Sheet: Parties

Exact headers:

| Column | Required | Example |
|---|---:|---|
| party_id | Yes | `SUP-001` |
| party_role | Yes | `SUPPLIER` |
| name | Yes | `Example Supplier Ltd.` |
| tax_number | No | `1234567890` |
| email | No | `info@supplier.example` |
| phone | No | `+90 212 000 00 00` |
| street | No | `Teknoloji Cad. No:10` |
| city | Yes | `İstanbul` |
| postal_code | No | `34000` |
| country_code | Yes | `TR` |

`party_role` values: `SUPPLIER`, `CUSTOMER`.

Role, country, currency, and unit codes are case-sensitive and are not automatically uppercased.

## Sheet: PaymentAccounts

Exact headers:

| Column | Required | Example |
|---|---:|---|
| payment_account_id | Yes | `PAY-001` |
| account_holder_name | Yes | `Example Supplier Ltd.` |
| bank_name | Yes | `Example Bank` |
| iban | Yes | `TR00TEST000000000000000000` |
| swift_code | No | `TESTTRIS` |
| currency_code | Yes | `TRY` |

Enter IBANs without spaces for XML interchange; see the [IBAN validation behavior](data-contract.md#4-payment-account).

## Sheet: Invoices

Exact headers:

| Column | Required | Example |
|---|---:|---|
| invoice_number | Yes | `INV-2026-0001` |
| issue_date | Yes | `2026-07-27` |
| due_date | No | `2026-08-26` |
| currency_code | Yes | `TRY` |
| supplier_id | Yes | `SUP-001` |
| customer_id | Yes | `CUS-001` |
| payment_account_id | Yes | `PAY-001` |
| note | No | `Temmuz danışmanlık hizmeti` |

## Sheet: InvoiceLines

Exact headers:

| Column | Required | Example |
|---|---:|---|
| invoice_number | Yes | `INV-2026-0001` |
| line_number | Yes | `1` |
| item_code | No | `SRV-001` |
| description | Yes | `Yazılım geliştirme hizmeti` |
| quantity | Yes | `2` |
| unit_code | Yes | `C62` |
| unit_price | Yes | `1250.00` |
| tax_rate | Yes | `20.00` |

## Example relationship

```text
Invoices.supplier_id          → Parties.party_id where role=SUPPLIER
Invoices.customer_id          → Parties.party_id where role=CUSTOMER
Invoices.payment_account_id   → PaymentAccounts.payment_account_id
InvoiceLines.invoice_number   → Invoices.invoice_number
```

## Invalid workbook examples

- Required sheet missing
- Header misspelled, reordered, or in the wrong case
- Duplicate `party_id`
- Invoice references a missing customer
- Customer reference points to a supplier-role party
- `due_date` before `issue_date`
- Invoice has no lines
- Line quantity is zero or negative
- Tax rate is above 100
- Numeric value is entered as `1.234,56`
- A contract data cell contains a formula, including a formula with a cached result
- A text identifier is stored as a numeric cell
- A row contains only an invoice number and no other required values

## Output and templates

The writer creates the four contract sheets with header styling, frozen header rows, filters, date/number formats, and input-validation helpers. It writes the supported domain fields; arbitrary extra sheets, workbook formatting, and formulas from an input file are not preserved by a conversion.

[templates/invoice-data-template.xlsx](../../templates/invoice-data-template.xlsx) is a blank starting workbook. The CLI `template --output <path>` command and the Swing template action generate a new blank workbook using the same writer. Populate the required data before converting a template to XML; the XML schema requires nonempty collections.
