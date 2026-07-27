package com.berk.dataintegration.xml;

import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.validation.InvoiceBatchValidator;
import com.berk.dataintegration.validation.ValidationError;
import com.berk.dataintegration.xml.dto.XmlInvoiceBatch;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class XmlInvoiceReader {
    private final JAXBContext jaxbContext;
    private final XmlSchemaValidator schemaValidator;
    private final XmlInvoiceBatchDomainMapper mapper;
    private final InvoiceBatchValidator batchValidator;
    private final XmlBusinessConsistencyValidator businessConsistencyValidator;

    public XmlInvoiceReader() {
        this(new XmlSchemaValidator(), new XmlInvoiceBatchDomainMapper(), new InvoiceBatchValidator(), new XmlBusinessConsistencyValidator());
    }

    XmlInvoiceReader(
            XmlSchemaValidator schemaValidator,
            XmlInvoiceBatchDomainMapper mapper,
            InvoiceBatchValidator batchValidator,
            XmlBusinessConsistencyValidator businessConsistencyValidator
    ) {
        this.schemaValidator = schemaValidator;
        this.mapper = mapper;
        this.batchValidator = batchValidator;
        this.businessConsistencyValidator = businessConsistencyValidator;
        try {
            this.jaxbContext = JAXBContext.newInstance(XmlInvoiceBatch.class);
        } catch (JAXBException exception) {
            throw new XmlReadException("Unable to initialize JAXB context.", exception);
        }
    }

    public XmlReadResult read(Path path) throws IOException {
        String sourceFile = sourceFile(path);
        List<ValidationError> schemaErrors = schemaValidator.validate(path);
        if (!schemaErrors.isEmpty()) {
            return new XmlReadResult(emptyBatch(), schemaErrors);
        }

        XmlInvoiceBatch xmlBatch = unmarshal(path, sourceFile);
        InvoiceBatch domainBatch = mapper.toDomain(xmlBatch);
        XmlSourceIndex sourceIndex = XmlSourceIndex.from(xmlBatch, sourceFile);
        List<ValidationError> errors = new ArrayList<>(batchValidator.validate(domainBatch).errors().stream()
                .map(sourceIndex::enrich)
                .toList());
        errors.addAll(businessConsistencyValidator.validate(xmlBatch, domainBatch, sourceFile));
        return new XmlReadResult(domainBatch, errors);
    }

    private XmlInvoiceBatch unmarshal(Path path, String sourceFile) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Object value = unmarshaller.unmarshal(new SAXSource(SecureXml.newXmlReader(), new InputSource(input)));
            return (XmlInvoiceBatch) value;
        } catch (JAXBException exception) {
            throw new XmlReadException("Unable to unmarshal XML after schema validation.", exception);
        } catch (XmlReadException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new XmlReadException("Unable to read XML source " + sourceFile + ".", exception);
        }
    }

    private InvoiceBatch emptyBatch() {
        return new InvoiceBatch(List.of(), List.of(), List.of());
    }

    private String sourceFile(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }
}
