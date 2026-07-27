package com.berk.dataintegration.xml;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class XmlSchemaValidator {
    private final Schema schema;

    public XmlSchemaValidator() {
        this(XmlSchemaProvider.invoiceDataSchema());
    }

    XmlSchemaValidator(Schema schema) {
        this.schema = schema;
    }

    public List<ValidationError> validate(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            schema.newValidator().validate(new SAXSource(SecureXml.newXmlReader(), new InputSource(input)));
            return List.of();
        } catch (SAXParseException exception) {
            return List.of(toError(path, exception));
        } catch (SAXException exception) {
            return List.of(XmlValidationErrors.xmlError(
                    classify(exception),
                    category(classify(exception)),
                    message(classify(exception)),
                    null,
                    null,
                    sourceFile(path),
                    null,
                    null,
                    null,
                    XmlValidationErrors.safeMessage(exception)
            ));
        }
    }

    private ValidationError toError(Path path, SAXParseException exception) {
        ValidationCode code = classify(exception);
        return XmlValidationErrors.fromParseException(
                code,
                category(code),
                message(code),
                sourceFile(path),
                exception
        );
    }

    private ValidationCode classify(Exception exception) {
        String message = XmlValidationErrors.safeMessage(exception).toLowerCase(Locale.ROOT);
        if (message.contains("doctype") || message.contains("external entity") || message.contains("external dtd")) {
            return ValidationCode.XML_SECURITY_VIOLATION;
        }
        if (message.startsWith("cvc-") || message.contains("identity constraint")) {
            return ValidationCode.XML_SCHEMA_VALIDATION_FAILED;
        }
        return ValidationCode.MALFORMED_XML;
    }

    private ValidationCategory category(ValidationCode code) {
        return code == ValidationCode.XML_SCHEMA_VALIDATION_FAILED ? ValidationCategory.CONTRACT : ValidationCategory.PARSE;
    }

    private String message(ValidationCode code) {
        return switch (code) {
            case XML_SECURITY_VIOLATION -> "XML input violates secure parsing rules.";
            case XML_SCHEMA_VALIDATION_FAILED -> "XML input does not conform to invoice-data-v1.xsd.";
            case MALFORMED_XML -> "XML input is not well formed.";
            default -> "XML validation failed.";
        };
    }

    private String sourceFile(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }
}
