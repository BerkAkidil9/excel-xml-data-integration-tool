package com.berk.dataintegration.service;

import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.excel.ExcelInvoiceReader;
import com.berk.dataintegration.excel.ExcelReadResult;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.xml.XmlInvoiceReader;
import com.berk.dataintegration.xml.XmlReadResult;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceConversionServiceTest {
    private static final String XML_NAMESPACE = "urn:berk:excel-xml-integration:invoice:v1";
    private final InvoiceConversionService service = new InvoiceConversionService();

    @TempDir
    Path tempDir;

    @Test
    void convertsExcelToXmlWithCountsAndCalculatedValues() throws Exception {
        Path input = Path.of("examples/valid-invoice-data.xlsx");
        Path output = tempDir.resolve("invoice-data.xml");

        ConversionResult result = service.convertExcelToXml(input, output);

        assertTrue(result.isSuccess(), () -> result.errors().toString());
        assertEquals(new ConversionRecordCounts(3, 2, 2, 3), result.recordCounts());
        assertTrue(Files.exists(output));

        ExcelReadResult source = new ExcelInvoiceReader().read(input);
        XmlReadResult generated = new XmlInvoiceReader().read(output);
        assertTrue(source.errors().isEmpty(), () -> source.errors().toString());
        assertTrue(generated.errors().isEmpty(), () -> generated.errors().toString());
        assertBatchSemantics(source.batch(), generated.batch());

        Document document = parseXml(output);
        assertEquals("3700.00", text(document, "/i:InvoiceBatch/i:Invoices/i:Invoice[1]/i:Totals/i:TaxExclusiveAmount"));
        assertEquals("740.00", text(document, "/i:InvoiceBatch/i:Invoices/i:Invoice[1]/i:Totals/i:TaxAmount"));
        assertEquals("4440.00", text(document, "/i:InvoiceBatch/i:Invoices/i:Invoice[1]/i:Totals/i:PayableAmount"));
        assertEquals("1000.00", text(document, "/i:InvoiceBatch/i:Invoices/i:Invoice[2]/i:Lines/i:Line/i:LineNetAmount"));
        assertEquals("0.00", text(document, "/i:InvoiceBatch/i:Invoices/i:Invoice[2]/i:Lines/i:Line/i:LineTaxAmount"));
    }

    @Test
    void convertsXmlToExcelWithCountsAndSupportedFields() throws IOException {
        Path input = Path.of("src/test/resources/fixtures/xml/valid-multiple-invoices.xml");
        Path output = tempDir.resolve("invoice-data.xlsx");

        ConversionResult result = service.convertXmlToExcel(input, output);

        assertTrue(result.isSuccess(), () -> result.errors().toString());
        assertEquals(new ConversionRecordCounts(3, 2, 2, 3), result.recordCounts());
        assertTrue(Files.exists(output));

        XmlReadResult source = new XmlInvoiceReader().read(input);
        ExcelReadResult generated = new ExcelInvoiceReader().read(output);
        assertTrue(source.errors().isEmpty(), () -> source.errors().toString());
        assertTrue(generated.errors().isEmpty(), () -> generated.errors().toString());
        assertBatchSemantics(source.batch(), generated.batch());
    }

    @Test
    void doesNotWriteXmlOutputWhenExcelValidationFails() throws IOException {
        Path input = invalidWorkbookMissingInvoiceLines();
        Path output = tempDir.resolve("failed.xml");
        Files.writeString(output, "existing", StandardCharsets.UTF_8);

        ConversionResult result = service.convertExcelToXml(input, output);

        assertFalse(result.isSuccess());
        assertEquals(ValidationCode.MISSING_REQUIRED_SHEET, result.errors().getFirst().code());
        assertEquals("existing", Files.readString(output, StandardCharsets.UTF_8));
        assertNoTemporaryFilesFor(output);
    }

    @Test
    void doesNotWriteExcelOutputWhenXmlBusinessValidationFails() throws IOException {
        Path input = Path.of("src/test/resources/fixtures/xml/invalid-business-wrong-total.xml");
        Path output = tempDir.resolve("failed.xlsx");

        ConversionResult result = service.convertXmlToExcel(input, output);

        assertFalse(result.isSuccess());
        assertEquals(ValidationCode.XML_INVOICE_TOTAL_MISMATCH, result.errors().getFirst().code());
        assertFalse(Files.exists(output));
        assertNoTemporaryFilesFor(output);
    }

    private Path invalidWorkbookMissingInvoiceLines() throws IOException {
        Path path = tempDir.resolve("missing-invoice-lines.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            createSheet(workbook, "Parties", "party_id", "party_role", "name", "tax_number", "email", "phone", "street", "city", "postal_code", "country_code");
            createSheet(workbook, "PaymentAccounts", "payment_account_id", "account_holder_name", "bank_name", "iban", "swift_code", "currency_code");
            createSheet(workbook, "Invoices", "invoice_number", "issue_date", "due_date", "currency_code", "supplier_id", "customer_id", "payment_account_id", "note");
            try (OutputStream output = Files.newOutputStream(path)) {
                workbook.write(output);
            }
        }
        return path;
    }

    private void createSheet(XSSFWorkbook workbook, String sheetName, String... headers) {
        org.apache.poi.ss.usermodel.Row header = workbook.createSheet(sheetName).createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
    }

    private void assertNoTemporaryFilesFor(Path output) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        String prefix = output.toAbsolutePath().getFileName() + ".";
        try (java.util.stream.Stream<Path> paths = Files.list(parent)) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().startsWith(prefix)));
        }
    }

    private void assertBatchSemantics(InvoiceBatch expected, InvoiceBatch actual) {
        assertEquals(expected.parties(), actual.parties());
        assertEquals(expected.paymentAccounts(), actual.paymentAccounts());
        assertEquals(expected.invoices().size(), actual.invoices().size());
        for (int i = 0; i < expected.invoices().size(); i++) {
            assertInvoiceSemantics(expected.invoices().get(i), actual.invoices().get(i));
        }
    }

    private void assertInvoiceSemantics(Invoice expected, Invoice actual) {
        assertEquals(expected.invoiceNumber(), actual.invoiceNumber());
        assertEquals(expected.issueDate(), actual.issueDate());
        assertEquals(expected.dueDate(), actual.dueDate());
        assertEquals(expected.currencyCode(), actual.currencyCode());
        assertEquals(expected.supplierId(), actual.supplierId());
        assertEquals(expected.customerId(), actual.customerId());
        assertEquals(expected.paymentAccountId(), actual.paymentAccountId());
        assertEquals(expected.note(), actual.note());
        assertEquals(expected.lines().size(), actual.lines().size());
        for (int i = 0; i < expected.lines().size(); i++) {
            assertLineSemantics(expected.lines().get(i), actual.lines().get(i));
        }
    }

    private void assertLineSemantics(InvoiceLine expected, InvoiceLine actual) {
        assertEquals(expected.lineNumber(), actual.lineNumber());
        assertEquals(expected.itemCode(), actual.itemCode());
        assertEquals(expected.description(), actual.description());
        assertDecimalEquals(expected.quantity(), actual.quantity());
        assertEquals(expected.unitCode(), actual.unitCode());
        assertDecimalEquals(expected.unitPrice(), actual.unitPrice());
        assertDecimalEquals(expected.taxRate(), actual.taxRate());
    }

    private void assertDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but got " + actual);
    }

    private Document parseXml(Path xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().parse(xml.toFile());
    }

    private String text(Document document, String expression) throws XPathExpressionException {
        return xpath().evaluate(expression, document);
    }

    private XPath xpath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                return "i".equals(prefix) ? XML_NAMESPACE : XMLConstants.NULL_NS_URI;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return XML_NAMESPACE.equals(namespaceURI) ? "i" : null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                return XML_NAMESPACE.equals(namespaceURI) ? List.of("i").iterator() : List.<String>of().iterator();
            }
        });
        return xpath;
    }
}
