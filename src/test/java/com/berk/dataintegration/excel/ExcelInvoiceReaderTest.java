package com.berk.dataintegration.excel;

import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelInvoiceReaderTest {
    private final ExcelInvoiceReader reader = new ExcelInvoiceReader();

    @TempDir
    Path tempDir;

    @Test
    void readsValidExampleWorkbook() throws IOException {
        ExcelReadResult result = reader.read(Path.of("examples/valid-invoice-data.xlsx"));

        assertTrue(result.errors().isEmpty(), () -> result.errors().toString());
        InvoiceBatch batch = result.batch();
        assertEquals(3, batch.parties().size());
        assertEquals(2, batch.paymentAccounts().size());
        assertEquals(2, batch.invoices().size());
        assertEquals("INV-2026-0001", batch.invoices().getFirst().invoiceNumber());
        assertEquals(LocalDate.of(2026, 7, 27), batch.invoices().getFirst().issueDate());
        assertEquals(LocalDate.of(2026, 8, 26), batch.invoices().getFirst().dueDate());
        assertEquals(2, batch.invoices().getFirst().lines().size());
        assertEquals(new BigDecimal("2"), batch.invoices().getFirst().lines().getFirst().quantity());
        assertEquals(new BigDecimal("1250"), batch.invoices().getFirst().lines().getFirst().unitPrice());
        assertEquals(new BigDecimal("20"), batch.invoices().getFirst().lines().getFirst().taxRate());
    }

    @Test
    void acceptsRealExcelDatesAndIsoDateText() throws IOException {
        Path workbook = workbook("real-date.xlsx", xlsx -> {
            CellStyle dateStyle = xlsx.createCellStyle();
            CreationHelper helper = xlsx.getCreationHelper();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
            Row invoice = sheet(xlsx, ExcelSheetDefinition.INVOICES).getRow(1);
            Cell issueDate = invoice.getCell(1);
            issueDate.setCellValue(LocalDateTime.of(2026, 7, 27, 0, 0));
            issueDate.setCellStyle(dateStyle);
            invoice.getCell(2).setCellValue("2026-08-26");
        });

        ExcelReadResult result = reader.read(workbook);

        assertTrue(result.errors().isEmpty(), () -> result.errors().toString());
        assertEquals(LocalDate.of(2026, 7, 27), result.batch().invoices().getFirst().issueDate());
        assertEquals(LocalDate.of(2026, 8, 26), result.batch().invoices().getFirst().dueDate());
    }

    @Test
    void reportsMissingRequiredSheet() throws IOException {
        Path workbook = workbook("missing-sheet.xlsx", xlsx -> {
            int index = xlsx.getSheetIndex(ExcelSheetDefinition.INVOICE_LINES.sheetName());
            xlsx.removeSheetAt(index);
        });

        ExcelReadResult result = reader.read(workbook);

        ValidationError error = onlyError(result.errors(), ValidationCode.MISSING_REQUIRED_SHEET);
        assertEquals("InvoiceLines", error.sheet());
        assertEquals("missing-sheet.xlsx", error.sourceFile());
    }

    @Test
    void reportsHeaderMismatchAndUnknownColumn() throws IOException {
        Path workbook = workbook("bad-header.xlsx", xlsx -> {
            Row header = sheet(xlsx, ExcelSheetDefinition.PARTIES).getRow(0);
            header.getCell(0).setCellValue("Party_ID");
            header.createCell(10).setCellValue("extra");
        });

        ExcelReadResult result = reader.read(workbook);

        ValidationError mismatch = onlyError(result.errors(), ValidationCode.HEADER_MISMATCH);
        assertEquals("Parties", mismatch.sheet());
        assertEquals(1, mismatch.row());
        assertEquals("A", mismatch.column());
        assertEquals("partyId", mismatch.field());
        assertEquals("Party_ID", mismatch.value());

        ValidationError unknownColumn = onlyError(result.errors(), ValidationCode.UNKNOWN_COLUMN);
        assertEquals("Parties", unknownColumn.sheet());
        assertEquals(1, unknownColumn.row());
        assertEquals("K", unknownColumn.column());
        assertEquals("extra", unknownColumn.value());
    }

    @Test
    void ignoresFullyBlankRowsAndValidatesPartialRows() throws IOException {
        Path workbook = workbook("partial-row.xlsx", xlsx -> {
            Sheet parties = sheet(xlsx, ExcelSheetDefinition.PARTIES);
            parties.createRow(2);
            Row partial = parties.createRow(3);
            partial.createCell(0).setCellValue("PARTIAL");
        });

        ExcelReadResult result = reader.read(workbook);

        assertFalse(result.errors().isEmpty());
        assertFalse(hasErrorAtRow(result.errors(), 3));
        ValidationError error = findError(result.errors(), ValidationCode.REQUIRED_FIELD, "role");
        assertEquals("Parties", error.sheet());
        assertEquals(4, error.row());
        assertEquals("B", error.column());
        assertEquals("PARTIAL", error.recordId());
    }

    @Test
    void rejectsInvalidCellTypeWithContext() throws IOException {
        Path workbook = workbook("invalid-cell-type.xlsx", xlsx -> {
            Cell description = sheet(xlsx, ExcelSheetDefinition.INVOICE_LINES).getRow(1).getCell(3);
            description.setCellValue(true);
        });

        ExcelReadResult result = reader.read(workbook);

        ValidationError error = onlyError(result.errors(), ValidationCode.INVALID_CELL_TYPE);
        assertEquals("InvoiceLines", error.sheet());
        assertEquals(2, error.row());
        assertEquals("D", error.column());
        assertEquals("description", error.field());
        assertEquals("TRUE", error.value());
    }

    @Test
    void rejectsLocaleDecimalTextWithContext() throws IOException {
        Path workbook = workbook("locale-decimal.xlsx", xlsx -> {
            Cell quantity = sheet(xlsx, ExcelSheetDefinition.INVOICE_LINES).getRow(1).getCell(4);
            quantity.setCellValue("1.234,56");
        });

        ExcelReadResult result = reader.read(workbook);

        ValidationError error = onlyError(result.errors(), ValidationCode.INVALID_DECIMAL_FORMAT);
        assertEquals("InvoiceLines", error.sheet());
        assertEquals(2, error.row());
        assertEquals("E", error.column());
        assertEquals("quantity", error.field());
        assertEquals("1.234,56", error.value());
    }

    @Test
    void reportsMissingInvoiceReferenceForOrphanLine() throws IOException {
        Path workbook = workbook("orphan-line.xlsx", xlsx -> {
            Cell invoiceNumber = sheet(xlsx, ExcelSheetDefinition.INVOICE_LINES).getRow(1).getCell(0);
            invoiceNumber.setCellValue("MISSING-INV");
        });

        ExcelReadResult result = reader.read(workbook);

        ValidationError error = onlyError(result.errors(), ValidationCode.MISSING_INVOICE_REFERENCE);
        assertEquals("InvoiceLines", error.sheet());
        assertEquals(2, error.row());
        assertEquals("A", error.column());
        assertEquals("invoiceNumber", error.field());
        assertEquals("MISSING-INV", error.value());
    }

    @Test
    void delegatesDuplicateAndReferenceErrorsWithSourceContext() throws IOException {
        Path workbook = workbook("delegated-errors.xlsx", xlsx -> {
            Row duplicateSupplier = sheet(xlsx, ExcelSheetDefinition.PARTIES).createRow(2);
            copyRow(sheet(xlsx, ExcelSheetDefinition.PARTIES).getRow(1), duplicateSupplier, 10);
            Row invoice = sheet(xlsx, ExcelSheetDefinition.INVOICES).getRow(1);
            invoice.getCell(5).setCellValue("CUS-MISSING");
        });

        ExcelReadResult result = reader.read(workbook);

        ValidationError duplicate = onlyError(result.errors(), ValidationCode.DUPLICATE_PARTY_ID);
        assertEquals("partyId", duplicate.field());
        assertEquals("SUP-001", duplicate.recordId());
        assertEquals("Parties", duplicate.sheet());
        assertEquals(3, duplicate.row());
        assertEquals("A", duplicate.column());
        assertEquals("SUP-001", duplicate.value());

        ValidationError missingCustomer = onlyError(result.errors(), ValidationCode.MISSING_CUSTOMER_REFERENCE);
        assertEquals("customerId", missingCustomer.field());
        assertEquals("INV-2026-0001", missingCustomer.recordId());
        assertEquals("Invoices", missingCustomer.sheet());
        assertEquals(2, missingCustomer.row());
        assertEquals("F", missingCustomer.column());
        assertEquals("CUS-MISSING", missingCustomer.value());
    }

    @Test
    void rejectsFormulaCells() throws IOException {
        Path workbook = workbook("formula.xlsx", xlsx -> {
            Cell quantity = sheet(xlsx, ExcelSheetDefinition.INVOICE_LINES).getRow(1).getCell(4);
            quantity.setCellFormula("1+1");
        });

        ExcelReadResult result = reader.read(workbook);

        ValidationError error = onlyError(result.errors(), ValidationCode.UNSUPPORTED_FORMULA);
        assertEquals("InvoiceLines", error.sheet());
        assertEquals(2, error.row());
        assertEquals("E", error.column());
        assertEquals("quantity", error.field());
        assertNotNull(error.value());
    }

    private Path workbook(String fileName, Consumer<XSSFWorkbook> customizer) throws IOException {
        Path path = tempDir.resolve(fileName);
        try (XSSFWorkbook workbook = validWorkbook()) {
            customizer.accept(workbook);
            try (OutputStream output = Files.newOutputStream(path)) {
                workbook.write(output);
            }
        }
        return path;
    }

    private XSSFWorkbook validWorkbook() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        createSheet(workbook, ExcelSheetDefinition.PARTIES,
                List.of(
                        List.of("SUP-001", "SUPPLIER", "Acme Supplier Ltd.", "1234567890", "billing@supplier.example", "+905551111111", "Supplier Street 1", "Istanbul", "34000", "TR"),
                        List.of("CUS-001", "CUSTOMER", "Example Customer A.S.", "9876543210", "accounts@customer.example", "+905552222222", "Customer Avenue 10", "Ankara", "06000", "TR")
                ));
        createSheet(workbook, ExcelSheetDefinition.PAYMENT_ACCOUNTS,
                List.of(List.of("PAY-001", "Acme Supplier Ltd.", "Example Bank", "TR00TEST000000000000000000", "TESTTRIS", "TRY")));
        createSheet(workbook, ExcelSheetDefinition.INVOICES,
                List.of(List.of("INV-2026-0001", "2026-07-27", "2026-08-26", "TRY", "SUP-001", "CUS-001", "PAY-001", "Test invoice")));
        createSheet(workbook, ExcelSheetDefinition.INVOICE_LINES,
                List.of(List.of("INV-2026-0001", 1, "SRV-001", "Consulting service", "2", "C62", "1250.00", "20.00")));
        return workbook;
    }

    private void createSheet(XSSFWorkbook workbook, ExcelSheetDefinition definition, List<List<Object>> dataRows) {
        Sheet sheet = workbook.createSheet(definition.sheetName());
        Row header = sheet.createRow(0);
        for (int i = 0; i < definition.columnCount(); i++) {
            header.createCell(i).setCellValue(definition.column(i).header());
        }
        for (int rowIndex = 0; rowIndex < dataRows.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<Object> values = dataRows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                setCell(row.createCell(columnIndex), values.get(columnIndex));
            }
        }
    }

    private void setCell(Cell cell, Object value) {
        if (value instanceof Integer integer) {
            cell.setCellValue(integer);
        } else if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private Sheet sheet(XSSFWorkbook workbook, ExcelSheetDefinition definition) {
        return workbook.getSheet(definition.sheetName());
    }

    private void copyRow(Row source, Row target, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            Cell sourceCell = source.getCell(i);
            Cell targetCell = target.createCell(i);
            targetCell.setCellValue(sourceCell.getStringCellValue());
        }
    }

    private ValidationError onlyError(List<ValidationError> errors, ValidationCode code) {
        List<ValidationError> matching = errors.stream()
                .filter(error -> error.code() == code)
                .toList();
        assertEquals(1, matching.size(), () -> "Expected one " + code + " error but got " + errors);
        return matching.getFirst();
    }

    private ValidationError findError(List<ValidationError> errors, ValidationCode code, String field) {
        return errors.stream()
                .filter(error -> error.code() == code)
                .filter(error -> field.equals(error.field()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing " + code + " for " + field + ": " + errors));
    }

    private boolean hasErrorAtRow(List<ValidationError> errors, int row) {
        return errors.stream().anyMatch(error -> Integer.valueOf(row).equals(error.row()));
    }
}
