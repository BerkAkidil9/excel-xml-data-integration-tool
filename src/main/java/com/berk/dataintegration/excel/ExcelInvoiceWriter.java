package com.berk.dataintegration.excel;

import com.berk.dataintegration.domain.Address;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.domain.Party;
import com.berk.dataintegration.domain.PartyRole;
import com.berk.dataintegration.domain.PaymentAccount;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ExcelInvoiceWriter {
    private static final int DATA_VALIDATION_LAST_ROW = 10_000;

    public void write(InvoiceBatch batch, Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(outputPath))) {
            write(batch, output);
        }
    }

    public void write(InvoiceBatch batch, OutputStream output) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            WorkbookStyles styles = createStyles(workbook);
            writeParties(workbook, batch, styles);
            writePaymentAccounts(workbook, batch, styles);
            writeInvoices(workbook, batch, styles);
            writeInvoiceLines(workbook, batch, styles);
            workbook.write(output);
        }
    }

    private void writeParties(Workbook workbook, InvoiceBatch batch, WorkbookStyles styles) {
        Sheet sheet = createContractSheet(workbook, ExcelSheetDefinition.PARTIES, styles);
        int rowIndex = 1;
        for (Party party : batch.parties()) {
            Row row = sheet.createRow(rowIndex++);
            writeText(row, 0, party.partyId());
            writeText(row, 1, role(party.role()));
            writeText(row, 2, party.name());
            writeText(row, 3, party.taxNumber());
            writeText(row, 4, party.email());
            writeText(row, 5, party.phone());
            Address address = party.address();
            writeText(row, 6, address == null ? null : address.street());
            writeText(row, 7, address == null ? null : address.city());
            writeText(row, 8, address == null ? null : address.postalCode());
            writeText(row, 9, address == null ? null : address.countryCode());
        }
        addExplicitListValidation(sheet, 1, "SUPPLIER", "CUSTOMER");
        finishSheet(sheet, ExcelSheetDefinition.PARTIES);
    }

    private void writePaymentAccounts(Workbook workbook, InvoiceBatch batch, WorkbookStyles styles) {
        Sheet sheet = createContractSheet(workbook, ExcelSheetDefinition.PAYMENT_ACCOUNTS, styles);
        int rowIndex = 1;
        for (PaymentAccount paymentAccount : batch.paymentAccounts()) {
            Row row = sheet.createRow(rowIndex++);
            writeText(row, 0, paymentAccount.paymentAccountId());
            writeText(row, 1, paymentAccount.accountHolderName());
            writeText(row, 2, paymentAccount.bankName());
            writeText(row, 3, paymentAccount.iban());
            writeText(row, 4, paymentAccount.swiftCode());
            writeText(row, 5, paymentAccount.currencyCode());
        }
        finishSheet(sheet, ExcelSheetDefinition.PAYMENT_ACCOUNTS);
    }

    private void writeInvoices(Workbook workbook, InvoiceBatch batch, WorkbookStyles styles) {
        Sheet sheet = createContractSheet(workbook, ExcelSheetDefinition.INVOICES, styles);
        int rowIndex = 1;
        for (Invoice invoice : batch.invoices()) {
            Row row = sheet.createRow(rowIndex++);
            writeText(row, 0, invoice.invoiceNumber());
            writeDate(row, 1, invoice.issueDate(), styles.dateStyle());
            writeDate(row, 2, invoice.dueDate(), styles.dateStyle());
            writeText(row, 3, invoice.currencyCode());
            writeText(row, 4, invoice.supplierId());
            writeText(row, 5, invoice.customerId());
            writeText(row, 6, invoice.paymentAccountId());
            writeText(row, 7, invoice.note());
        }
        finishSheet(sheet, ExcelSheetDefinition.INVOICES);
    }

    private void writeInvoiceLines(Workbook workbook, InvoiceBatch batch, WorkbookStyles styles) {
        Sheet sheet = createContractSheet(workbook, ExcelSheetDefinition.INVOICE_LINES, styles);
        int rowIndex = 1;
        for (Invoice invoice : batch.invoices()) {
            for (InvoiceLine line : invoice.lines()) {
                Row row = sheet.createRow(rowIndex++);
                writeText(row, 0, invoice.invoiceNumber());
                writeInteger(row, 1, line.lineNumber());
                writeText(row, 2, line.itemCode());
                writeText(row, 3, line.description());
                writeDecimal(row, 4, line.quantity());
                writeText(row, 5, line.unitCode());
                writeDecimal(row, 6, line.unitPrice());
                writeDecimal(row, 7, line.taxRate());
            }
        }
        finishSheet(sheet, ExcelSheetDefinition.INVOICE_LINES);
    }

    private Sheet createContractSheet(Workbook workbook, ExcelSheetDefinition definition, WorkbookStyles styles) {
        Sheet sheet = workbook.createSheet(definition.sheetName());
        Row header = sheet.createRow(0);
        for (int i = 0; i < definition.columnCount(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(definition.column(i).header());
            cell.setCellStyle(styles.headerStyle());
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, definition.columnCount() - 1));
        return sheet;
    }

    private void finishSheet(Sheet sheet, ExcelSheetDefinition definition) {
        for (int i = 0; i < definition.columnCount(); i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(Math.max(currentWidth, 12 * 256), 40 * 256));
        }
    }

    private void writeText(Row row, int columnIndex, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        row.createCell(columnIndex).setCellValue(value);
    }

    private void writeInteger(Row row, int columnIndex, int value) {
        row.createCell(columnIndex).setCellValue(value);
    }

    private void writeDecimal(Row row, int columnIndex, BigDecimal value) {
        if (value == null) {
            return;
        }
        row.createCell(columnIndex).setCellValue(value.doubleValue());
    }

    private void writeDate(Row row, int columnIndex, LocalDate value, CellStyle dateStyle) {
        if (value == null) {
            return;
        }
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(LocalDateTime.of(value.getYear(), value.getMonth(), value.getDayOfMonth(), 0, 0));
        cell.setCellStyle(dateStyle);
    }

    private String role(PartyRole role) {
        return role == null ? null : role.name();
    }

    private void addExplicitListValidation(Sheet sheet, int columnIndex, String... values) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        CellRangeAddressList addressList = new CellRangeAddressList(1, DATA_VALIDATION_LAST_ROW, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private WorkbookStyles createStyles(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper creationHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));

        return new WorkbookStyles(headerStyle, dateStyle);
    }

    private record WorkbookStyles(
            CellStyle headerStyle,
            CellStyle dateStyle
    ) {
    }
}
