package com.berk.dataintegration.service;

import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.excel.ExcelInvoiceReader;
import com.berk.dataintegration.excel.ExcelInvoiceWriter;
import com.berk.dataintegration.excel.ExcelReadResult;
import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import com.berk.dataintegration.xml.XmlInvoiceReader;
import com.berk.dataintegration.xml.XmlInvoiceWriter;
import com.berk.dataintegration.xml.XmlReadResult;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class InvoiceConversionService {
    private final ExcelInvoiceReader excelReader;
    private final ExcelInvoiceWriter excelWriter;
    private final XmlInvoiceReader xmlReader;
    private final XmlInvoiceWriter xmlWriter;

    public InvoiceConversionService() {
        this(new ExcelInvoiceReader(), new ExcelInvoiceWriter(), new XmlInvoiceReader(), new XmlInvoiceWriter());
    }

    InvoiceConversionService(
            ExcelInvoiceReader excelReader,
            ExcelInvoiceWriter excelWriter,
            XmlInvoiceReader xmlReader,
            XmlInvoiceWriter xmlWriter
    ) {
        this.excelReader = excelReader;
        this.excelWriter = excelWriter;
        this.xmlReader = xmlReader;
        this.xmlWriter = xmlWriter;
    }

    public ConversionResult convertExcelToXml(Path inputPath, Path outputPath) {
        return convert(inputPath, outputPath, this::readExcel, xmlWriter::write);
    }

    public ConversionResult convertXmlToExcel(Path inputPath, Path outputPath) {
        return convert(inputPath, outputPath, this::readXml, excelWriter::write);
    }

    private ConversionResult convert(
            Path inputPath,
            Path outputPath,
            ReadOperation reader,
            WriteOperation writer
    ) {
        ReadBatchResult readResult;
        try {
            readResult = reader.read(inputPath);
        } catch (IOException | RuntimeException exception) {
            return new ConversionResult(
                    inputPath,
                    outputPath,
                    ConversionRecordCounts.empty(),
                    List.of(ioError(ValidationCode.CONVERSION_INPUT_FAILED, inputPath, exception))
            );
        }

        ConversionRecordCounts counts = ConversionRecordCounts.from(readResult.batch());
        if (!readResult.errors().isEmpty()) {
            return new ConversionResult(inputPath, outputPath, counts, readResult.errors());
        }

        Path temporaryOutput = null;
        try {
            temporaryOutput = createTemporaryOutput(outputPath);
            writer.write(readResult.batch(), temporaryOutput);
            replaceOutput(temporaryOutput, outputPath);
            temporaryOutput = null;
            return new ConversionResult(inputPath, outputPath, counts, List.of());
        } catch (IOException | RuntimeException exception) {
            deleteQuietly(temporaryOutput);
            return new ConversionResult(
                    inputPath,
                    outputPath,
                    counts,
                    List.of(ioError(ValidationCode.CONVERSION_OUTPUT_FAILED, outputPath, exception))
            );
        } finally {
            deleteQuietly(temporaryOutput);
        }
    }

    private ReadBatchResult readExcel(Path inputPath) throws IOException {
        ExcelReadResult result = excelReader.read(inputPath);
        return new ReadBatchResult(result.batch(), result.errors());
    }

    private ReadBatchResult readXml(Path inputPath) throws IOException {
        XmlReadResult result = xmlReader.read(inputPath);
        return new ReadBatchResult(result.batch(), result.errors());
    }

    private Path createTemporaryOutput(Path outputPath) throws IOException {
        Path absoluteOutput = outputPath.toAbsolutePath();
        Path parent = absoluteOutput.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String fileName = absoluteOutput.getFileName().toString();
        return Files.createTempFile(parent, fileName + ".", ".tmp");
    }

    private void replaceOutput(Path temporaryOutput, Path outputPath) throws IOException {
        Path absoluteOutput = outputPath.toAbsolutePath();
        try {
            Files.move(
                    temporaryOutput,
                    absoluteOutput,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryOutput, absoluteOutput, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ValidationError ioError(ValidationCode code, Path path, Exception exception) {
        return new ValidationError(
                code,
                ValidationCategory.IO,
                code == ValidationCode.CONVERSION_INPUT_FAILED
                        ? "Conversion input could not be read."
                        : "Conversion output could not be written.",
                null,
                null,
                sourceFile(path),
                null,
                null,
                null,
                exception.getMessage()
        );
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup after a failed conversion attempt.
        }
    }

    private String sourceFile(Path path) {
        if (path == null) {
            return null;
        }
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }

    private record ReadBatchResult(
            InvoiceBatch batch,
            List<ValidationError> errors
    ) {
    }

    @FunctionalInterface
    private interface ReadOperation {
        ReadBatchResult read(Path path) throws IOException;
    }

    @FunctionalInterface
    private interface WriteOperation {
        void write(InvoiceBatch batch, Path path) throws IOException;
    }
}
