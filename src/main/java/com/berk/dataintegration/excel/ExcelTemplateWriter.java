package com.berk.dataintegration.excel;

import com.berk.dataintegration.domain.InvoiceBatch;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

public final class ExcelTemplateWriter {
    private final ExcelInvoiceWriter invoiceWriter;

    public ExcelTemplateWriter() {
        this(new ExcelInvoiceWriter());
    }

    ExcelTemplateWriter(ExcelInvoiceWriter invoiceWriter) {
        this.invoiceWriter = invoiceWriter;
    }

    public void write(Path outputPath) throws IOException {
        invoiceWriter.write(emptyBatch(), outputPath);
    }

    public void write(OutputStream output) throws IOException {
        invoiceWriter.write(emptyBatch(), output);
    }

    private InvoiceBatch emptyBatch() {
        return new InvoiceBatch(List.of(), List.of(), List.of());
    }
}
