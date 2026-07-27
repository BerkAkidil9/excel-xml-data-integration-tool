package com.berk.dataintegration.excel;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ExcelWorkbookContractValidator {
    private final DataFormatter formatter = new DataFormatter(Locale.ROOT, false);

    public List<ValidationError> validate(Workbook workbook, String sourceFile) {
        List<ValidationError> errors = new ArrayList<>();
        for (ExcelSheetDefinition definition : ExcelSheetDefinition.values()) {
            Sheet sheet = workbook.getSheet(definition.sheetName());
            if (sheet == null) {
                errors.add(ExcelValidationErrors.error(
                        ValidationCode.MISSING_REQUIRED_SHEET,
                        ValidationCategory.CONTRACT,
                        "Required sheet is missing.",
                        null,
                        null,
                        sourceFile,
                        definition.sheetName(),
                        null,
                        null,
                        null
                ));
                continue;
            }
            validateHeader(sheet, definition, sourceFile, errors);
        }
        return errors;
    }

    private void validateHeader(
            Sheet sheet,
            ExcelSheetDefinition definition,
            String sourceFile,
            List<ValidationError> errors
    ) {
        Row header = sheet.getRow(0);
        for (int i = 0; i < definition.columnCount(); i++) {
            String actual = headerValue(header, i);
            String expected = definition.column(i).header();
            if (!expected.equals(actual)) {
                errors.add(ExcelValidationErrors.error(
                        ValidationCode.HEADER_MISMATCH,
                        ValidationCategory.CONTRACT,
                        "Header must match the Excel contract exactly.",
                        definition.column(i).field(),
                        null,
                        sourceFile,
                        definition.sheetName(),
                        1,
                        CellReference.convertNumToColString(i),
                        actual
                ));
            }
        }

        if (header == null || header.getLastCellNum() <= definition.columnCount()) {
            return;
        }
        for (int i = definition.columnCount(); i < header.getLastCellNum(); i++) {
            String value = headerValue(header, i);
            if (value != null && !value.isBlank()) {
                errors.add(ExcelValidationErrors.error(
                        ValidationCode.UNKNOWN_COLUMN,
                        ValidationCategory.CONTRACT,
                        "Unknown extra column is not allowed in a required sheet.",
                        null,
                        null,
                        sourceFile,
                        definition.sheetName(),
                        1,
                        CellReference.convertNumToColString(i),
                        value
                ));
            }
        }
    }

    private String headerValue(Row row, int columnIndex) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }
}
