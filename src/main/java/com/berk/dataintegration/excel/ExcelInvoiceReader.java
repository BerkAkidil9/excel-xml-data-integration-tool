package com.berk.dataintegration.excel;

import com.berk.dataintegration.domain.Address;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.domain.Party;
import com.berk.dataintegration.domain.PartyRole;
import com.berk.dataintegration.domain.PaymentAccount;
import com.berk.dataintegration.validation.InvoiceBatchValidator;
import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExcelInvoiceReader {
    private final ExcelWorkbookContractValidator contractValidator;
    private final ExcelCellParser cellParser;
    private final InvoiceBatchValidator batchValidator;

    public ExcelInvoiceReader() {
        this(new ExcelWorkbookContractValidator(), new InvoiceBatchValidator());
    }

    ExcelInvoiceReader(ExcelWorkbookContractValidator contractValidator, InvoiceBatchValidator batchValidator) {
        this.contractValidator = contractValidator;
        this.cellParser = new ExcelCellParser();
        this.batchValidator = batchValidator;
    }

    public ExcelReadResult read(Path path) throws IOException {
        String sourceFile = sourceFile(path);
        if (!sourceFile.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            return new ExcelReadResult(emptyBatch(), List.of(ExcelValidationErrors.error(
                    ValidationCode.INVALID_FILE_EXTENSION,
                    ValidationCategory.CONTRACT,
                    "Only .xlsx files are supported.",
                    null,
                    null,
                    sourceFile,
                    null,
                    null,
                    null,
                    sourceFile
            )));
        }

        try (InputStream input = Files.newInputStream(path);
             Workbook workbook = WorkbookFactory.create(input)) {
            List<ValidationError> errors = new ArrayList<>(contractValidator.validate(workbook, sourceFile));
            if (!errors.isEmpty()) {
                return new ExcelReadResult(emptyBatch(), errors);
            }

            ExcelSourceIndex sourceIndex = new ExcelSourceIndex();
            List<Party> parties = readParties(workbook.getSheet(ExcelSheetDefinition.PARTIES.sheetName()), sourceFile, errors, sourceIndex);
            List<PaymentAccount> paymentAccounts = readPaymentAccounts(
                    workbook.getSheet(ExcelSheetDefinition.PAYMENT_ACCOUNTS.sheetName()),
                    sourceFile,
                    errors,
                    sourceIndex
            );
            ParsedInvoices parsedInvoices = readInvoices(
                    workbook.getSheet(ExcelSheetDefinition.INVOICES.sheetName()),
                    sourceFile,
                    errors,
                    sourceIndex
            );
            attachInvoiceLines(
                    workbook.getSheet(ExcelSheetDefinition.INVOICE_LINES.sheetName()),
                    sourceFile,
                    errors,
                    sourceIndex,
                    parsedInvoices
            );

            InvoiceBatch batch = new InvoiceBatch(parties, paymentAccounts, parsedInvoices.toInvoices());
            batchValidator.validate(batch).errors().stream()
                    .map(sourceIndex::enrich)
                    .forEach(errors::add);
            return new ExcelReadResult(batch, errors);
        }
    }

    private List<Party> readParties(
            Sheet sheet,
            String sourceFile,
            List<ValidationError> errors,
            ExcelSourceIndex sourceIndex
    ) {
        List<Party> parties = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (cellParser.isContractBlank(row, ExcelSheetDefinition.PARTIES)) {
                continue;
            }

            String partyId = text(row, ExcelSheetDefinition.PARTIES, 0, null, sourceFile, errors);
            PartyRole role = partyRole(row, ExcelSheetDefinition.PARTIES, 1, partyId, sourceFile, errors);
            String name = text(row, ExcelSheetDefinition.PARTIES, 2, partyId, sourceFile, errors);
            String taxNumber = text(row, ExcelSheetDefinition.PARTIES, 3, partyId, sourceFile, errors);
            String email = text(row, ExcelSheetDefinition.PARTIES, 4, partyId, sourceFile, errors);
            String phone = text(row, ExcelSheetDefinition.PARTIES, 5, partyId, sourceFile, errors);
            String street = text(row, ExcelSheetDefinition.PARTIES, 6, partyId, sourceFile, errors);
            String city = text(row, ExcelSheetDefinition.PARTIES, 7, partyId, sourceFile, errors);
            String postalCode = text(row, ExcelSheetDefinition.PARTIES, 8, partyId, sourceFile, errors);
            String countryCode = text(row, ExcelSheetDefinition.PARTIES, 9, partyId, sourceFile, errors);

            indexRow(sourceIndex, row, ExcelSheetDefinition.PARTIES, partyId, sourceFile);
            parties.add(new Party(partyId, role, name, taxNumber, email, phone,
                    new Address(street, city, postalCode, countryCode)));
        }
        return parties;
    }

    private List<PaymentAccount> readPaymentAccounts(
            Sheet sheet,
            String sourceFile,
            List<ValidationError> errors,
            ExcelSourceIndex sourceIndex
    ) {
        List<PaymentAccount> paymentAccounts = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (cellParser.isContractBlank(row, ExcelSheetDefinition.PAYMENT_ACCOUNTS)) {
                continue;
            }

            String paymentAccountId = text(row, ExcelSheetDefinition.PAYMENT_ACCOUNTS, 0, null, sourceFile, errors);
            String accountHolderName = text(row, ExcelSheetDefinition.PAYMENT_ACCOUNTS, 1, paymentAccountId, sourceFile, errors);
            String bankName = text(row, ExcelSheetDefinition.PAYMENT_ACCOUNTS, 2, paymentAccountId, sourceFile, errors);
            String iban = text(row, ExcelSheetDefinition.PAYMENT_ACCOUNTS, 3, paymentAccountId, sourceFile, errors);
            String swiftCode = text(row, ExcelSheetDefinition.PAYMENT_ACCOUNTS, 4, paymentAccountId, sourceFile, errors);
            String currencyCode = text(row, ExcelSheetDefinition.PAYMENT_ACCOUNTS, 5, paymentAccountId, sourceFile, errors);

            indexRow(sourceIndex, row, ExcelSheetDefinition.PAYMENT_ACCOUNTS, paymentAccountId, sourceFile);
            paymentAccounts.add(new PaymentAccount(paymentAccountId, accountHolderName, bankName, iban, swiftCode, currencyCode));
        }
        return paymentAccounts;
    }

    private ParsedInvoices readInvoices(
            Sheet sheet,
            String sourceFile,
            List<ValidationError> errors,
            ExcelSourceIndex sourceIndex
    ) {
        List<ParsedInvoice> invoices = new ArrayList<>();
        Map<String, ParsedInvoice> firstByInvoiceNumber = new LinkedHashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (cellParser.isContractBlank(row, ExcelSheetDefinition.INVOICES)) {
                continue;
            }

            String invoiceNumber = text(row, ExcelSheetDefinition.INVOICES, 0, null, sourceFile, errors);
            LocalDate issueDate = date(row, ExcelSheetDefinition.INVOICES, 1, invoiceNumber, sourceFile, errors);
            LocalDate dueDate = date(row, ExcelSheetDefinition.INVOICES, 2, invoiceNumber, sourceFile, errors);
            String currencyCode = text(row, ExcelSheetDefinition.INVOICES, 3, invoiceNumber, sourceFile, errors);
            String supplierId = text(row, ExcelSheetDefinition.INVOICES, 4, invoiceNumber, sourceFile, errors);
            String customerId = text(row, ExcelSheetDefinition.INVOICES, 5, invoiceNumber, sourceFile, errors);
            String paymentAccountId = text(row, ExcelSheetDefinition.INVOICES, 6, invoiceNumber, sourceFile, errors);
            String note = text(row, ExcelSheetDefinition.INVOICES, 7, invoiceNumber, sourceFile, errors);

            indexRow(sourceIndex, row, ExcelSheetDefinition.INVOICES, invoiceNumber, sourceFile);
            ParsedInvoice parsedInvoice = new ParsedInvoice(
                    invoiceNumber,
                    issueDate,
                    dueDate,
                    currencyCode,
                    supplierId,
                    customerId,
                    paymentAccountId,
                    note
            );
            invoices.add(parsedInvoice);
            if (invoiceNumber != null && !invoiceNumber.isBlank()) {
                firstByInvoiceNumber.putIfAbsent(invoiceNumber, parsedInvoice);
            }
        }
        return new ParsedInvoices(invoices, firstByInvoiceNumber);
    }

    private void attachInvoiceLines(
            Sheet sheet,
            String sourceFile,
            List<ValidationError> errors,
            ExcelSourceIndex sourceIndex,
            ParsedInvoices parsedInvoices
    ) {
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (cellParser.isContractBlank(row, ExcelSheetDefinition.INVOICE_LINES)) {
                continue;
            }

            String invoiceNumber = text(row, ExcelSheetDefinition.INVOICE_LINES, 0, null, sourceFile, errors);
            Integer lineNumberValue = integer(row, ExcelSheetDefinition.INVOICE_LINES, 1, invoiceNumber, sourceFile, errors);
            int lineNumber = lineNumberValue == null ? 0 : lineNumberValue;
            String lineRecordId = lineRecordId(invoiceNumber, lineNumber);
            String itemCode = text(row, ExcelSheetDefinition.INVOICE_LINES, 2, lineRecordId, sourceFile, errors);
            String description = text(row, ExcelSheetDefinition.INVOICE_LINES, 3, lineRecordId, sourceFile, errors);
            BigDecimal quantity = decimal(row, ExcelSheetDefinition.INVOICE_LINES, 4, lineRecordId, sourceFile, errors);
            String unitCode = text(row, ExcelSheetDefinition.INVOICE_LINES, 5, lineRecordId, sourceFile, errors);
            BigDecimal unitPrice = decimal(row, ExcelSheetDefinition.INVOICE_LINES, 6, lineRecordId, sourceFile, errors);
            BigDecimal taxRate = decimal(row, ExcelSheetDefinition.INVOICE_LINES, 7, lineRecordId, sourceFile, errors);

            indexRow(sourceIndex, row, ExcelSheetDefinition.INVOICE_LINES, lineRecordId, sourceFile);
            if (invoiceNumber == null || invoiceNumber.isBlank()) {
                errors.add(ExcelValidationErrors.cellError(
                        ValidationCode.REQUIRED_FIELD,
                        ValidationCategory.FIELD,
                        "Invoice line invoice number is required.",
                        ExcelSheetDefinition.INVOICE_LINES.column(0),
                        lineRecordId,
                        sourceFile,
                        ExcelSheetDefinition.INVOICE_LINES.sheetName(),
                        rowIndex,
                        0,
                        diagnostic(row, 0)
                ));
                continue;
            }

            ParsedInvoice invoice = parsedInvoices.firstByInvoiceNumber().get(invoiceNumber);
            if (invoice == null) {
                errors.add(ExcelValidationErrors.cellError(
                        ValidationCode.MISSING_INVOICE_REFERENCE,
                        ValidationCategory.REFERENCE,
                        "Invoice line must reference an existing invoice.",
                        ExcelSheetDefinition.INVOICE_LINES.column(0),
                        lineRecordId,
                        sourceFile,
                        ExcelSheetDefinition.INVOICE_LINES.sheetName(),
                        rowIndex,
                        0,
                        diagnostic(row, 0)
                ));
                continue;
            }
            invoice.lines().add(new InvoiceLine(lineNumber, itemCode, description, quantity, unitCode, unitPrice, taxRate));
        }
    }

    private String text(
            Row row,
            ExcelSheetDefinition definition,
            int columnIndex,
            String recordId,
            String sourceFile,
            List<ValidationError> errors
    ) {
        return cellParser.parseText(cell(row, columnIndex), definition.column(columnIndex), recordId,
                sourceFile, definition.sheetName(), errors);
    }

    private PartyRole partyRole(
            Row row,
            ExcelSheetDefinition definition,
            int columnIndex,
            String recordId,
            String sourceFile,
            List<ValidationError> errors
    ) {
        return cellParser.parsePartyRole(cell(row, columnIndex), definition.column(columnIndex), recordId,
                sourceFile, definition.sheetName(), errors);
    }

    private LocalDate date(
            Row row,
            ExcelSheetDefinition definition,
            int columnIndex,
            String recordId,
            String sourceFile,
            List<ValidationError> errors
    ) {
        return cellParser.parseDate(cell(row, columnIndex), definition.column(columnIndex), recordId,
                sourceFile, definition.sheetName(), errors);
    }

    private BigDecimal decimal(
            Row row,
            ExcelSheetDefinition definition,
            int columnIndex,
            String recordId,
            String sourceFile,
            List<ValidationError> errors
    ) {
        return cellParser.parseDecimal(cell(row, columnIndex), definition.column(columnIndex), recordId,
                sourceFile, definition.sheetName(), errors);
    }

    private Integer integer(
            Row row,
            ExcelSheetDefinition definition,
            int columnIndex,
            String recordId,
            String sourceFile,
            List<ValidationError> errors
    ) {
        return cellParser.parseInteger(cell(row, columnIndex), definition.column(columnIndex), recordId,
                sourceFile, definition.sheetName(), errors);
    }

    private void indexRow(
            ExcelSourceIndex sourceIndex,
            Row row,
            ExcelSheetDefinition definition,
            String recordId,
            String sourceFile
    ) {
        if (recordId == null || row == null) {
            return;
        }
        for (int i = 0; i < definition.columnCount(); i++) {
            ExcelColumn column = definition.column(i);
            sourceIndex.put(recordId, column.field(), ExcelValidationErrors.sourceLocation(
                    sourceFile,
                    definition.sheetName(),
                    row.getRowNum(),
                    i,
                    column,
                    diagnostic(row, i)
            ));
        }
    }

    private String diagnostic(Row row, int columnIndex) {
        return cellParser.diagnosticValue(cell(row, columnIndex));
    }

    private Cell cell(Row row, int columnIndex) {
        if (row == null) {
            return null;
        }
        return row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    }

    private String lineRecordId(String invoiceNumber, int lineNumber) {
        String prefix = invoiceNumber == null || invoiceNumber.isBlank() ? "InvoiceLines" : invoiceNumber;
        return prefix + "#" + lineNumber;
    }

    private String sourceFile(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }

    private InvoiceBatch emptyBatch() {
        return new InvoiceBatch(List.of(), List.of(), List.of());
    }

    private record ParsedInvoices(
            List<ParsedInvoice> invoices,
            Map<String, ParsedInvoice> firstByInvoiceNumber
    ) {
        List<Invoice> toInvoices() {
            return invoices.stream().map(ParsedInvoice::toInvoice).toList();
        }
    }

    private record ParsedInvoice(
            String invoiceNumber,
            LocalDate issueDate,
            LocalDate dueDate,
            String currencyCode,
            String supplierId,
            String customerId,
            String paymentAccountId,
            String note,
            List<InvoiceLine> lines
    ) {
        ParsedInvoice(
                String invoiceNumber,
                LocalDate issueDate,
                LocalDate dueDate,
                String currencyCode,
                String supplierId,
                String customerId,
                String paymentAccountId,
                String note
        ) {
            this(invoiceNumber, issueDate, dueDate, currencyCode, supplierId, customerId, paymentAccountId,
                    note, new ArrayList<>());
        }

        Invoice toInvoice() {
            return new Invoice(invoiceNumber, issueDate, dueDate, currencyCode, supplierId, customerId,
                    paymentAccountId, note, lines);
        }
    }
}
