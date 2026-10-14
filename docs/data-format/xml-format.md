# XML Format — invoice-data-v1

## Identity

- Encoding: UTF-8
- Namespace: `urn:berk:excel-xml-integration:invoice:v1`
- Root element: `InvoiceBatch`
- Required version attribute: `1.0`
- XSD: [invoice-data-v1.xsd](../../src/main/resources/schema/invoice-data-v1.xsd)

This is a project-specific internal exchange format. It is not UBL, UBL-TR, Peppol, or GİB e-Fatura.

The reader, writer, and schema were reviewed on 2026-10-14. The schema is bundled in the application jar; validation does not download an external schema.

## Document order

The root children appear in this exact order:

1. `Parties`
2. `PaymentAccounts`
3. `Invoices`

Each invoice has this order:

1. `InvoiceNumber`
2. `IssueDate`
3. optional `DueDate`
4. `CurrencyCode`
5. `SupplierPartyId`
6. `CustomerPartyId`
7. `PaymentAccountId`
8. optional `Note`
9. `Lines`
10. `Totals`

The writer omits blank optional text and absent dates instead of writing empty tags such as `<Note/>`. Each root collection must contain at least one record; each invoice must contain at least one line.

`InvoiceLines.invoice_number` is an Excel join column. XML expresses that relationship by nesting each `Line` under its parent `Invoice/Lines`, so the invoice number is not repeated inside a line.

## Complete example

See [examples/valid-invoice-data.xml](../../examples/valid-invoice-data.xml).

A shortened example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<InvoiceBatch xmlns="urn:berk:excel-xml-integration:invoice:v1" version="1.0">
  <Parties>
    <Party>
      <PartyId>SUP-001</PartyId>
      <Role>SUPPLIER</Role>
      <Name>Example Supplier Ltd.</Name>
      <Address>
        <City>İstanbul</City>
        <CountryCode>TR</CountryCode>
      </Address>
    </Party>
    <Party>
      <PartyId>CUS-001</PartyId>
      <Role>CUSTOMER</Role>
      <Name>Example Customer A.S.</Name>
      <Address>
        <City>Ankara</City>
        <CountryCode>TR</CountryCode>
      </Address>
    </Party>
  </Parties>
  <PaymentAccounts>
    <PaymentAccount>
      <PaymentAccountId>PAY-001</PaymentAccountId>
      <AccountHolderName>Example Supplier Ltd.</AccountHolderName>
      <BankName>Example Bank</BankName>
      <IBAN>TR00TEST000000000000000000</IBAN>
      <CurrencyCode>TRY</CurrencyCode>
    </PaymentAccount>
  </PaymentAccounts>
  <Invoices>
    <Invoice>
      <InvoiceNumber>INV-2026-0001</InvoiceNumber>
      <IssueDate>2026-07-27</IssueDate>
      <CurrencyCode>TRY</CurrencyCode>
      <SupplierPartyId>SUP-001</SupplierPartyId>
      <CustomerPartyId>CUS-001</CustomerPartyId>
      <PaymentAccountId>PAY-001</PaymentAccountId>
      <Lines>
        <Line>
          <LineNumber>1</LineNumber>
          <Description>Yazılım geliştirme hizmeti</Description>
          <Quantity>2</Quantity>
          <UnitCode>C62</UnitCode>
          <UnitPrice>1250.00</UnitPrice>
          <TaxRate>20.00</TaxRate>
          <LineNetAmount>2500.00</LineNetAmount>
          <LineTaxAmount>500.00</LineTaxAmount>
        </Line>
      </Lines>
      <Totals>
        <TaxExclusiveAmount>2500.00</TaxExclusiveAmount>
        <TaxAmount>500.00</TaxAmount>
        <PayableAmount>3000.00</PayableAmount>
      </Totals>
    </Invoice>
  </Invoices>
</InvoiceBatch>
```

## Validation stages

### Stage 1 — secure XML parsing

- DOCTYPE forbidden
- external general entities disabled
- external parameter entities disabled
- external DTD and schema access disabled
- XInclude disabled and secure processing enabled
- no external entity or schema fetching

XSD validation and subsequent JAXB unmarshalling both use the secure SAX reader. Malformed/forbidden XML and schema violations produce structured validation errors; an invalid schema result stops processing before domain conversion.

### Stage 2 — XSD validation

Checks:

- required elements
- element order
- basic types and patterns
- unique party, payment-account, and invoice identifiers
- references to existing IDs

### Stage 3 — domain/business validation

Checks not fully expressible in XSD:

- party role matches supplier/customer reference
- supplier and customer differ
- due date is not before issue date
- invoice/payment-account currency matches
- line numbers are unique within an invoice
- monetary totals match recalculation

Line net and tax amounts are rounded independently to two decimal places with `HALF_UP`; tax is calculated from the unrounded `quantity × unitPrice` value. Invoice totals sum the rounded line amounts. See the [shared calculation rules](data-contract.md#7-calculated-values).

The writer also validates generated XML against the bundled XSD during JAXB marshalling. Conversion through `InvoiceConversionService` writes the result to a temporary file before replacing the destination.

## Invalid XML fixtures

Fixtures are under [src/test/resources/fixtures/xml](../../src/test/resources/fixtures/xml/):

- [invalid-schema-missing-invoice-number.xml](../../src/test/resources/fixtures/xml/invalid-schema-missing-invoice-number.xml): structurally invalid
- [invalid-business-wrong-total.xml](../../src/test/resources/fixtures/xml/invalid-business-wrong-total.xml): XSD-valid shape but incorrect calculated total
- [invalid-reference.xml](../../src/test/resources/fixtures/xml/invalid-reference.xml): references a missing party ID

Malformed XML, altered line totals, and a DOCTYPE/XXE payload are also generated inside `XmlInvoiceReaderTest`; they are not separate fixture files. These regression checks do not replace a dependency vulnerability scan.
