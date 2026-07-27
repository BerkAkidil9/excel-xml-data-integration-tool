package com.berk.dataintegration.xml;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;

final class XmlSchemaProvider {
    private static final String SCHEMA_RESOURCE = "/schema/invoice-data-v1.xsd";

    private XmlSchemaProvider() {
    }

    static Schema invoiceDataSchema() {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            try (InputStream schemaStream = XmlSchemaProvider.class.getResourceAsStream(SCHEMA_RESOURCE)) {
                if (schemaStream == null) {
                    throw new XmlWriteException("Bundled XML schema was not found: " + SCHEMA_RESOURCE, null);
                }
                return schemaFactory.newSchema(new StreamSource(schemaStream));
            }
        } catch (SAXException | RuntimeException exception) {
            throw new XmlWriteException("Unable to load bundled XML schema.", exception);
        } catch (Exception exception) {
            throw new XmlWriteException("Unable to read bundled XML schema.", exception);
        }
    }
}
