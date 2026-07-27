package com.berk.dataintegration.xml;

import com.berk.dataintegration.TestBatches;
import com.berk.dataintegration.domain.Address;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.domain.Party;
import com.berk.dataintegration.domain.PartyRole;
import com.berk.dataintegration.domain.PaymentAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlInvoiceWriterTest {
    private static final String NAMESPACE = "urn:berk:excel-xml-integration:invoice:v1";
    private final XmlInvoiceWriter writer = new XmlInvoiceWriter();

    @TempDir
    Path tempDir;

    @Test
    void writesSchemaValidXmlWithContractNamespaceAndOrder() throws Exception {
        Path output = tempDir.resolve("invoice-data.xml");

        writer.write(TestBatches.validBatch(), output);

        assertValidAgainstSchema(output);
        Document document = parse(output);
        Element root = document.getDocumentElement();
        assertEquals(NAMESPACE, root.getNamespaceURI());
        assertEquals("InvoiceBatch", root.getLocalName());
        assertEquals("1.0", root.getAttribute("version"));
        assertEquals(List.of("Parties", "PaymentAccounts", "Invoices"), childElementNames(root));
        assertEquals(
                List.of(
                        "InvoiceNumber",
                        "IssueDate",
                        "DueDate",
                        "CurrencyCode",
                        "SupplierPartyId",
                        "CustomerPartyId",
                        "PaymentAccountId",
                        "Note",
                        "Lines",
                        "Totals"
                ),
                childElementNames(firstElement(document, "/i:InvoiceBatch/i:Invoices/i:Invoice[1]"))
        );
    }

    @Test
    void writesCalculatedLineAndInvoiceAmounts() throws Exception {
        Path output = tempDir.resolve("calculated.xml");

        writer.write(TestBatches.validBatch(), output);

        assertValidAgainstSchema(output);
        Document document = parse(output);
        assertEquals("2500.00", text(document, "/i:InvoiceBatch/i:Invoices/i:Invoice/i:Lines/i:Line/i:LineNetAmount"));
        assertEquals("500.00", text(document, "/i:InvoiceBatch/i:Invoices/i:Invoice/i:Lines/i:Line/i:LineTaxAmount"));
        assertEquals("2500.00", text(document, "/i:InvoiceBatch/i:Invoices/i:Invoice/i:Totals/i:TaxExclusiveAmount"));
        assertEquals("500.00", text(document, "/i:InvoiceBatch/i:Invoices/i:Invoice/i:Totals/i:TaxAmount"));
        assertEquals("3000.00", text(document, "/i:InvoiceBatch/i:Invoices/i:Invoice/i:Totals/i:PayableAmount"));
    }

    @Test
    void writesHalfUpRoundedCalculatedAmounts() throws Exception {
        InvoiceBatch batch = new InvoiceBatch(
                List.of(TestBatches.supplier(), TestBatches.customer()),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(TestBatches.invoice(
                        "INV-ROUND",
                        "SUP-001",
                        "CUS-001",
                        "PAY-001",
                        "TRY",
                        List.of(TestBatches.line(1, "1", "0.015", "100.00"))
                ))
        );
        Path output = tempDir.resolve("rounded.xml");

        writer.write(batch, output);

        assertValidAgainstSchema(output);
        Document document = parse(output);
        assertEquals("0.02", text(document, "//i:LineNetAmount"));
        assertEquals("0.02", text(document, "//i:LineTaxAmount"));
        assertEquals("0.02", text(document, "//i:TaxExclusiveAmount"));
        assertEquals("0.02", text(document, "//i:TaxAmount"));
        assertEquals("0.04", text(document, "//i:PayableAmount"));
    }

    @Test
    void omitsOptionalEmptyValues() throws Exception {
        InvoiceBatch batch = new InvoiceBatch(
                List.of(
                        new Party("SUP-001", PartyRole.SUPPLIER, "Tedarikci Ltd.", "", "", "", new Address("", "Istanbul", "", "TR")),
                        new Party("CUS-001", PartyRole.CUSTOMER, "Musteri A.S.", null, null, null, new Address(null, "Ankara", null, "TR"))
                ),
                List.of(new PaymentAccount("PAY-001", "Tedarikci Ltd.", "Example Bank", "TR00TEST000000000000000000", "", "TRY")),
                List.of(new Invoice(
                        "INV-OPTIONAL",
                        LocalDate.of(2026, 7, 27),
                        null,
                        "TRY",
                        "SUP-001",
                        "CUS-001",
                        "PAY-001",
                        "",
                        List.of(new InvoiceLine(1, "", "Danismanlik hizmeti", new BigDecimal("1"), "C62", new BigDecimal("100.00"), new BigDecimal("20.00")))
                ))
        );
        Path output = tempDir.resolve("optional.xml");

        writer.write(batch, output);

        assertValidAgainstSchema(output);
        Document document = parse(output);
        assertEquals(0, count(document, "//i:TaxNumber"));
        assertEquals(0, count(document, "//i:Email"));
        assertEquals(0, count(document, "//i:Phone"));
        assertEquals(0, count(document, "//i:Street"));
        assertEquals(0, count(document, "//i:PostalCode"));
        assertEquals(0, count(document, "//i:SwiftCode"));
        assertEquals(0, count(document, "//i:DueDate"));
        assertEquals(0, count(document, "//i:Note"));
        assertEquals(0, count(document, "//i:ItemCode"));
    }

    @Test
    void writesUtf8DeclarationAndFormattedOutput() throws Exception {
        InvoiceBatch batch = new InvoiceBatch(
                List.of(
                        new Party("SUP-001", PartyRole.SUPPLIER, "Tedarikci Ltd.", null, null, null, new Address(null, "İstanbul", null, "TR")),
                        TestBatches.customer()
                ),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(new Invoice(
                        "INV-UTF8",
                        LocalDate.of(2026, 7, 27),
                        LocalDate.of(2026, 8, 26),
                        "TRY",
                        "SUP-001",
                        "CUS-001",
                        "PAY-001",
                        "Türkçe açıklama",
                        List.of(TestBatches.line(1))
                ))
        );
        Path output = tempDir.resolve("utf8.xml");

        writer.write(batch, output);

        assertValidAgainstSchema(output);
        String xml = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(xml.startsWith("<?xml"));
        assertTrue(xml.contains("encoding=\"UTF-8\""));
        assertTrue(xml.contains("\n"));
        assertTrue(xml.contains("İstanbul"));
        assertTrue(xml.contains("Türkçe açıklama"));
    }

    @Test
    void rejectsGeneratedXmlThatDoesNotValidateAgainstSchema() {
        InvoiceBatch batch = new InvoiceBatch(
                List.of(TestBatches.supplier(), TestBatches.customer()),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(new Invoice(
                        null,
                        LocalDate.of(2026, 7, 27),
                        null,
                        "TRY",
                        "SUP-001",
                        "CUS-001",
                        "PAY-001",
                        null,
                        List.of(TestBatches.line(1))
                ))
        );

        assertThrows(XmlWriteException.class, () -> writer.write(batch, tempDir.resolve("invalid.xml")));
    }

    private void assertValidAgainstSchema(Path xml) throws SAXException, IOException {
        Schema schema = schema();
        schema.newValidator().validate(new StreamSource(xml.toFile()));
    }

    private Schema schema() throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try (InputStream stream = getClass().getResourceAsStream("/schema/invoice-data-v1.xsd")) {
            return schemaFactory.newSchema(new StreamSource(stream));
        }
    }

    private Document parse(Path xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory.newDocumentBuilder().parse(xml.toFile());
    }

    private Element firstElement(Document document, String expression) throws XPathExpressionException {
        return (Element) xpath().evaluate(expression, document, XPathConstants.NODE);
    }

    private String text(Document document, String expression) throws XPathExpressionException {
        return xpath().evaluate(expression, document);
    }

    private int count(Document document, String expression) throws XPathExpressionException {
        Number number = (Number) xpath().evaluate("count(" + expression + ")", document, XPathConstants.NUMBER);
        return number.intValue();
    }

    private XPath xpath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if ("i".equals(prefix)) {
                    return NAMESPACE;
                }
                return XMLConstants.NULL_NS_URI;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return NAMESPACE.equals(namespaceURI) ? "i" : null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                return NAMESPACE.equals(namespaceURI) ? List.of("i").iterator() : List.<String>of().iterator();
            }
        });
        return xpath;
    }

    private List<String> childElementNames(Element element) {
        NodeList childNodes = element.getChildNodes();
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element child) {
                names.add(child.getLocalName());
            }
        }
        return names;
    }
}
