package com.berk.dataintegration.xml;

import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.xml.dto.XmlInvoiceBatch;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import javax.xml.validation.Schema;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class XmlInvoiceWriter {
    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    private final JAXBContext jaxbContext;
    private final XmlInvoiceBatchMapper mapper;
    private final Schema schema;

    public XmlInvoiceWriter() {
        this(new XmlInvoiceBatchMapper(), XmlSchemaProvider.invoiceDataSchema());
    }

    XmlInvoiceWriter(XmlInvoiceBatchMapper mapper, Schema schema) {
        this.mapper = mapper;
        this.schema = schema;
        try {
            this.jaxbContext = JAXBContext.newInstance(XmlInvoiceBatch.class);
        } catch (JAXBException exception) {
            throw new XmlWriteException("Unable to initialize JAXB context.", exception);
        }
    }

    public void write(InvoiceBatch batch, Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            marshal(batch, writer);
        }
    }

    public String writeToString(InvoiceBatch batch) {
        StringWriter writer = new StringWriter();
        marshal(batch, writer);
        return writer.toString();
    }

    private void marshal(InvoiceBatch batch, Writer writer) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setSchema(schema);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, UTF_8);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(mapper.toXml(batch), writer);
        } catch (JAXBException exception) {
            throw new XmlWriteException("Generated XML does not conform to invoice-data-v1.", exception);
        }
    }
}
