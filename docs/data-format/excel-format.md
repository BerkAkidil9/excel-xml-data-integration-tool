# Excel Format — invoice-data-v1

## General workbook rules

- File extension: `.xlsx`
- Required sheets: `Parties`, `PaymentAccounts`, `Invoices`, `InvoiceLines`
- Optional informational sheets: `README`, `ReferenceLists`
- Required sheet names are case-sensitive.
- Header names are exact, lowercase `snake_case`, and case-sensitive after trimming surrounding whitespace.
- Header row is row 1.
- Data starts at row 2.
- Unknown extra sheets may be ignored.
- Unknown extra columns in a required sheet are contract errors.
- A fully empty row is ignored.
- A partially populated row is validated and may fail; it is never silently skipped.
- Formula cells are not required in input. The application should reject unsupported formula values when a reliable cached value is unavailable.

## Date input

The reader accepts either:

1. a real Excel date cell, or
2. ISO text `yyyy-MM-dd`.

The writer produces real Excel date cells formatted as `yyyy-mm-dd`.

## Decimal input

- Numeric Excel cells are preferred.
- ISO-style decimal text using `.` may also be accepted.
- Locale-specific values such as `1.234,56` should be rejected in v1 to avoid ambiguity.
- The writer produces numeric cells.

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
- Header misspelled, reordered with an unknown name, or wrong case
- Duplicate `party_id`
- Invoice references a missing customer
- Customer reference points to a supplier-role party
- `due_date` before `issue_date`
- Invoice has no lines
- Line quantity is zero or negative
- Tax rate is above 100
- Numeric value is entered as `1.234,56`
- A row contains only an invoice number and no other required values
