package com.berk.dataintegration.xml;

import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlInvoiceReaderTest {
    private final XmlInvoiceReader reader = new XmlInvoiceReader();

    @TempDir
    Path tempDir;

    @Test
    void readsValidXmlIntoDomain() throws IOException {
        XmlReadResult result = reader.read(fixture("valid-invoice-data.xml"));

        assertTrue(result.errors().isEmpty(), () -> result.errors().toString());
        InvoiceBatch batch = result.batch();
        assertEquals(2, batch.parties().size());
        assertEquals(1, batch.paymentAccounts().size());
        assertEquals(1, batch.invoices().size());
        assertEquals("SUP-001", batch.parties().getFirst().partyId());
        assertEquals("İstanbul", batch.parties().getFirst().address().city());
        assertEquals("INV-001", batch.invoices().getFirst().invoiceNumber());
        assertEquals("Service", batch.invoices().getFirst().lines().getFirst().description());
    }

    @Test
    void reportsXsdErrorForMissingRequiredElement() throws IOException {
        XmlReadResult result = reader.read(fixture("invalid-schema-missing-invoice-number.xml"));

        ValidationError error = onlyError(result.errors(), ValidationCode.XML_SCHEMA_VALIDATION_FAILED);
        assertEquals("invalid-schema-missing-invoice-number.xml", error.sourceFile());
        assertTrue(error.value().contains("InvoiceNumber"), () -> error.toString());
        assertTrue(result.batch().invoices().isEmpty());
    }

    @Test
    void reportsXsdErrorForInvalidReference() throws IOException {
        XmlReadResult result = reader.read(fixture("invalid-reference.xml"));

        ValidationError error = onlyError(result.errors(), ValidationCode.XML_SCHEMA_VALIDATION_FAILED);
        assertEquals("invalid-reference.xml", error.sourceFile());
        assertTrue(error.value().contains("CUS-MISSING") || error.value().contains("CustomerPartyRef"), () -> error.toString());
        assertTrue(result.batch().invoices().isEmpty());
    }

    @Test
    void reportsBusinessErrorForWrongInvoiceTotal() throws IOException {
        XmlReadResult result = reader.read(fixture("invalid-business-wrong-total.xml"));

        ValidationError error = onlyError(result.errors(), ValidationCode.XML_INVOICE_TOTAL_MISMATCH);
        assertEquals("invalid-business-wrong-total.xml", error.sourceFile());
        assertEquals("payableAmount", error.field());
        assertEquals("INV-001", error.recordId());
        assertEquals("/InvoiceBatch/Invoices/Invoice[1]/Totals/PayableAmount", error.sheet());
        assertEquals("999.00", error.value());
        assertEquals(1, result.batch().invoices().size());
    }

    @Test
    void reportsBusinessErrorForWrongLineTotal() throws IOException {
        String xml = Files.readString(fixture("valid-invoice-data.xml"), StandardCharsets.UTF_8)
                .replace("<LineNetAmount>100.00</LineNetAmount>", "<LineNetAmount>101.00</LineNetAmount>");
        Path invalid = writeTemp("wrong-line-total.xml", xml);

        XmlReadResult result = reader.read(invalid);

        ValidationError error = onlyError(result.errors(), ValidationCode.XML_LINE_TOTAL_MISMATCH);
        assertEquals("wrong-line-total.xml", error.sourceFile());
        assertEquals("lineNetAmount", error.field());
        assertEquals("INV-001#1", error.recordId());
        assertEquals("/InvoiceBatch/Invoices/Invoice[1]/Lines/Line[1]/LineNetAmount", error.sheet());
        assertEquals("101.00", error.value());
    }

    @Test
    void enrichesXmlDomainValidationErrorsWithPathAndValue() throws IOException {
        String xml = Files.readString(fixture("valid-invoice-data.xml"), StandardCharsets.UTF_8)
                .replace("<SupplierPartyId>SUP-001</SupplierPartyId>", "<SupplierPartyId>CUS-001</SupplierPartyId>");
        Path invalid = writeTemp("invalid-supplier-role.xml", xml);

        XmlReadResult result = reader.read(invalid);

        ValidationError error = onlyError(result.errors(), ValidationCode.INVALID_SUPPLIER_ROLE);
        assertEquals(ValidationCategory.REFERENCE, error.category());
        assertEquals("invalid-supplier-role.xml", error.sourceFile());
        assertEquals("supplierId", error.field());
        assertEquals("INV-001", error.recordId());
        assertEquals("/InvoiceBatch/Invoices/Invoice[1]/SupplierPartyId", error.sheet());
        assertEquals("CUS-001", error.value());
        assertTrue(result.errors().stream().anyMatch(candidate -> candidate.code() == ValidationCode.SUPPLIER_CUSTOMER_SAME),
                () -> result.errors().toString());
    }

    @Test
    void readsUtf8Text() throws IOException {
        XmlReadResult result = reader.read(fixture("valid-multiple-invoices.xml"));

        assertTrue(result.errors().isEmpty(), () -> result.errors().toString());
        assertEquals("İstanbul", result.batch().parties().getFirst().address().city());
        assertEquals("Temmuz ayı yazılım hizmetleri", result.batch().invoices().getFirst().note());
        assertEquals("Yazılım geliştirme hizmeti", result.batch().invoices().getFirst().lines().getFirst().description());
    }

    @Test
    void rejectsMalformedXmlSeparatelyFromXsdErrors() throws IOException {
        Path malformed = writeTemp("malformed.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <InvoiceBatch xmlns="urn:berk:excel-xml-integration:invoice:v1" version="1.0">
                  <Parties>
                """);

        XmlReadResult result = reader.read(malformed);

        ValidationError error = onlyError(result.errors(), ValidationCode.MALFORMED_XML);
        assertEquals("malformed.xml", error.sourceFile());
        assertFalse(error.value().isBlank());
    }

    @Test
    void rejectsXxePayloadBeforeEntityResolution() throws IOException {
        Path xxe = writeTemp("xxe.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE InvoiceBatch [
                  <!ENTITY xxe SYSTEM "file:///etc/passwd">
                ]>
                <InvoiceBatch xmlns="urn:berk:excel-xml-integration:invoice:v1" version="1.0">
                  <Parties>
                    <Party><PartyId>SUP-001</PartyId><Role>SUPPLIER</Role><Name>&xxe;</Name><Address><City>Istanbul</City><CountryCode>TR</CountryCode></Address></Party>
                  </Parties>
                  <PaymentAccounts>
                    <PaymentAccount><PaymentAccountId>PAY-001</PaymentAccountId><AccountHolderName>Supplier</AccountHolderName><BankName>Bank</BankName><IBAN>TR00TEST000000000000000000</IBAN><CurrencyCode>TRY</CurrencyCode></PaymentAccount>
                  </PaymentAccounts>
                  <Invoices>
                    <Invoice>
                      <InvoiceNumber>INV-001</InvoiceNumber><IssueDate>2026-07-27</IssueDate><CurrencyCode>TRY</CurrencyCode><SupplierPartyId>SUP-001</SupplierPartyId><CustomerPartyId>SUP-001</CustomerPartyId><PaymentAccountId>PAY-001</PaymentAccountId>
                      <Lines><Line><LineNumber>1</LineNumber><Description>Service</Description><Quantity>1</Quantity><UnitCode>C62</UnitCode><UnitPrice>100.00</UnitPrice><TaxRate>20.00</TaxRate><LineNetAmount>100.00</LineNetAmount><LineTaxAmount>20.00</LineTaxAmount></Line></Lines>
                      <Totals><TaxExclusiveAmount>100.00</TaxExclusiveAmount><TaxAmount>20.00</TaxAmount><PayableAmount>120.00</PayableAmount></Totals>
                    </Invoice>
                  </Invoices>
                </InvoiceBatch>
                """);

        XmlReadResult result = reader.read(xxe);

        ValidationError error = onlyError(result.errors(), ValidationCode.XML_SECURITY_VIOLATION);
        assertEquals("xxe.xml", error.sourceFile());
        assertTrue(error.value().toLowerCase(java.util.Locale.ROOT).contains("doctype"), () -> error.toString());
    }

    private Path fixture(String fileName) {
        return Path.of("src/test/resources/fixtures/xml", fileName);
    }

    private Path writeTemp(String fileName, String xml) throws IOException {
        Path path = tempDir.resolve(fileName);
        Files.writeString(path, xml, StandardCharsets.UTF_8);
        return path;
    }

    private ValidationError onlyError(List<ValidationError> errors, ValidationCode code) {
        List<ValidationError> matching = errors.stream()
                .filter(error -> error.code() == code)
                .toList();
        assertEquals(1, matching.size(), () -> "Expected one " + code + " error but got " + errors);
        return matching.getFirst();
    }
}
