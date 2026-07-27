package com.berk.dataintegration.excel;

import com.berk.dataintegration.TestBatches;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLine;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelInvoiceWriterTest {
    private final ExcelInvoiceWriter writer = new ExcelInvoiceWriter();

    @TempDir
    Path tempDir;

    @Test
    void writesRequiredSheetsAndExactHeaders() throws IOException {
        Path workbookPath = write(TestBatches.validBatch(), "headers.xlsx");

        try (Workbook workbook = open(workbookPath)) {
            assertEquals(4, workbook.getNumberOfSheets());
            assertEquals(List.of("Parties", "PaymentAccounts", "Invoices", "InvoiceLines"), sheetNames(workbook));
            for (ExcelSheetDefinition definition : ExcelSheetDefinition.values()) {
                assertHeaders(workbook.getSheet(definition.sheetName()), definition);
            }
            assertEquals(8, workbook.getSheet("InvoiceLines").getRow(0).getLastCellNum());
            assertEquals("tax_rate", workbook.getSheet("InvoiceLines").getRow(0).getCell(7).getStringCellValue());
        }
    }

    @Test
    void writesRealDateAndNumericCellsWithoutCalculatedColumns() throws IOException {
        Path workbookPath = write(TestBatches.validBatch(), "types.xlsx");

        try (Workbook workbook = open(workbookPath)) {
            Sheet invoices = workbook.getSheet("Invoices");
            Row invoice = invoices.getRow(1);
            assertDateCell(invoice.getCell(1), LocalDate.of(2026, 7, 27));
            assertDateCell(invoice.getCell(2), LocalDate.of(2026, 8, 26));

            Sheet lines = workbook.getSheet("InvoiceLines");
            Row line = lines.getRow(1);
            assertNumericCell(line.getCell(1), new BigDecimal("1"));
            assertNumericCell(line.getCell(4), new BigDecimal("2"));
            assertNumericCell(line.getCell(6), new BigDecimal("1250"));
            assertNumericCell(line.getCell(7), new BigDecimal("20"));
            assertEquals(8, lines.getRow(0).getLastCellNum());
        }
    }

    @Test
    void appliesHeaderStylingFreezePanesFiltersAndUsefulValidation() throws IOException {
        Path workbookPath = write(TestBatches.validBatch(), "features.xlsx");

        try (Workbook workbook = open(workbookPath)) {
            for (ExcelSheetDefinition definition : ExcelSheetDefinition.values()) {
                Sheet sheet = workbook.getSheet(definition.sheetName());
                assertNotNull(sheet.getPaneInformation());
                assertTrue(sheet.getPaneInformation().isFreezePane());
                assertTrue(((XSSFSheet) sheet).getCTWorksheet().isSetAutoFilter());
                int fontIndex = sheet.getRow(0).getCell(0).getCellStyle().getFontIndex();
                assertTrue(workbook.getFontAt(fontIndex).getBold());
            }
            assertEquals(1, workbook.getSheet("Parties").getDataValidations().size());
        }
    }

    @Test
    void generatedWorkbookRoundTripsThroughExcelReader() throws IOException {
        InvoiceBatch original = TestBatches.validBatch();
        Path workbookPath = write(original, "round-trip.xlsx");

        ExcelReadResult result = new ExcelInvoiceReader().read(workbookPath);

        assertTrue(result.errors().isEmpty(), () -> result.errors().toString());
        InvoiceBatch actual = result.batch();
        assertEquals(original.parties(), actual.parties());
        assertEquals(original.paymentAccounts(), actual.paymentAccounts());
        assertInvoiceSemantics(original.invoices().getFirst(), actual.invoices().getFirst());
    }

    private Path write(InvoiceBatch batch, String fileName) throws IOException {
        Path path = tempDir.resolve(fileName);
        writer.write(batch, path);
        return path;
    }

    private Workbook open(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return WorkbookFactory.create(input);
        }
    }

    private List<String> sheetNames(Workbook workbook) {
        return java.util.stream.IntStream.range(0, workbook.getNumberOfSheets())
                .mapToObj(workbook::getSheetName)
                .toList();
    }

    private void assertHeaders(Sheet sheet, ExcelSheetDefinition definition) {
        Row header = sheet.getRow(0);
        assertNotNull(header);
        assertEquals(definition.columnCount(), header.getLastCellNum());
        for (int i = 0; i < definition.columnCount(); i++) {
            assertEquals(definition.column(i).header(), header.getCell(i).getStringCellValue());
        }
    }

    private void assertDateCell(Cell cell, LocalDate expected) {
        assertEquals(CellType.NUMERIC, cell.getCellType());
        assertTrue(DateUtil.isCellDateFormatted(cell));
        assertEquals(expected, cell.getLocalDateTimeCellValue().toLocalDate());
    }

    private void assertNumericCell(Cell cell, BigDecimal expected) {
        assertEquals(CellType.NUMERIC, cell.getCellType());
        assertEquals(0, expected.compareTo(BigDecimal.valueOf(cell.getNumericCellValue())));
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
        assertLineSemantics(expected.lines().getFirst(), actual.lines().getFirst());
    }

    private void assertLineSemantics(InvoiceLine expected, InvoiceLine actual) {
        assertEquals(expected.lineNumber(), actual.lineNumber());
        assertEquals(expected.itemCode(), actual.itemCode());
        assertEquals(expected.description(), actual.description());
        assertEquals(0, expected.quantity().compareTo(actual.quantity()));
        assertEquals(expected.unitCode(), actual.unitCode());
        assertEquals(0, expected.unitPrice().compareTo(actual.unitPrice()));
        assertEquals(0, expected.taxRate().compareTo(actual.taxRate()));
    }
}
