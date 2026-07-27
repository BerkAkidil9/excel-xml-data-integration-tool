package com.berk.dataintegration.excel;

import com.berk.dataintegration.domain.PartyRole;
import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.NumberToTextConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

final class ExcelCellParser {
    private static final Pattern ISO_DECIMAL_TEXT = Pattern.compile("[+-]?\\d+(\\.\\d+)?");
    private static final Pattern INTEGER_TEXT = Pattern.compile("[+-]?\\d+");
    private final org.apache.poi.ss.usermodel.DataFormatter formatter =
            new org.apache.poi.ss.usermodel.DataFormatter(Locale.ROOT, false);

    boolean isContractBlank(Row row, ExcelSheetDefinition definition) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < definition.columnCount(); i++) {
            if (!isBlank(row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))) {
                return false;
            }
        }
        return true;
    }

    String parseText(
            Cell cell,
            ExcelColumn column,
            String recordId,
            String sourceFile,
            String sheet,
            List<ValidationError> errors
    ) {
        if (isBlank(cell)) {
            return null;
        }
        CellType type = effectiveType(cell);
        if (type == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            return value.isEmpty() ? null : value;
        }
        if (type == CellType.FORMULA) {
            addUnsupportedFormula(cell, column, recordId, sourceFile, sheet, errors);
            return null;
        }
        addInvalidCellType(cell, column, recordId, sourceFile, sheet, "Expected text cell.", errors);
        return null;
    }

    PartyRole parsePartyRole(
            Cell cell,
            ExcelColumn column,
            String recordId,
            String sourceFile,
            String sheet,
            List<ValidationError> errors
    ) {
        String value = parseText(cell, column, recordId, sourceFile, sheet, errors);
        if (value == null) {
            return null;
        }
        try {
            return PartyRole.valueOf(value);
        } catch (IllegalArgumentException exception) {
            errors.add(ExcelValidationErrors.cellError(
                    ValidationCode.INVALID_ENUM_VALUE,
                    ValidationCategory.PARSE,
                    "Party role must be SUPPLIER or CUSTOMER.",
                    column,
                    recordId,
                    sourceFile,
                    sheet,
                    cell.getRowIndex(),
                    cell.getColumnIndex(),
                    diagnosticValue(cell)
            ));
            return null;
        }
    }

    LocalDate parseDate(
            Cell cell,
            ExcelColumn column,
            String recordId,
            String sourceFile,
            String sheet,
            List<ValidationError> errors
    ) {
        if (isBlank(cell)) {
            return null;
        }
        CellType type = effectiveType(cell);
        if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        if (type == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            if (value.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException exception) {
                errors.add(ExcelValidationErrors.cellError(
                        ValidationCode.INVALID_DATE_FORMAT,
                        ValidationCategory.PARSE,
                        "Date must be a real Excel date cell or ISO text yyyy-MM-dd.",
                        column,
                        recordId,
                        sourceFile,
                        sheet,
                        cell.getRowIndex(),
                        cell.getColumnIndex(),
                        diagnosticValue(cell)
                ));
                return null;
            }
        }
        if (type == CellType.FORMULA) {
            addUnsupportedFormula(cell, column, recordId, sourceFile, sheet, errors);
            return null;
        }
        errors.add(ExcelValidationErrors.cellError(
                ValidationCode.INVALID_DATE_FORMAT,
                ValidationCategory.PARSE,
                "Date must be a real Excel date cell or ISO text yyyy-MM-dd.",
                column,
                recordId,
                sourceFile,
                sheet,
                cell.getRowIndex(),
                cell.getColumnIndex(),
                diagnosticValue(cell)
        ));
        return null;
    }

    BigDecimal parseDecimal(
            Cell cell,
            ExcelColumn column,
            String recordId,
            String sourceFile,
            String sheet,
            List<ValidationError> errors
    ) {
        if (isBlank(cell)) {
            return null;
        }
        CellType type = effectiveType(cell);
        if (type == CellType.NUMERIC) {
            return new BigDecimal(NumberToTextConverter.toText(cell.getNumericCellValue()));
        }
        if (type == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            if (value.isEmpty()) {
                return null;
            }
            if (!ISO_DECIMAL_TEXT.matcher(value).matches()) {
                errors.add(ExcelValidationErrors.cellError(
                        ValidationCode.INVALID_DECIMAL_FORMAT,
                        ValidationCategory.PARSE,
                        "Decimal text must use '.' as the decimal separator.",
                        column,
                        recordId,
                        sourceFile,
                        sheet,
                        cell.getRowIndex(),
                        cell.getColumnIndex(),
                        diagnosticValue(cell)
                ));
                return null;
            }
            return new BigDecimal(value);
        }
        if (type == CellType.FORMULA) {
            addUnsupportedFormula(cell, column, recordId, sourceFile, sheet, errors);
            return null;
        }
        addInvalidCellType(cell, column, recordId, sourceFile, sheet, "Expected numeric or ISO decimal text cell.", errors);
        return null;
    }

    Integer parseInteger(
            Cell cell,
            ExcelColumn column,
            String recordId,
            String sourceFile,
            String sheet,
            List<ValidationError> errors
    ) {
        if (isBlank(cell)) {
            return null;
        }
        CellType type = effectiveType(cell);
        if (type == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (value == Math.rint(value) && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return (int) value;
            }
            addInvalidInteger(cell, column, recordId, sourceFile, sheet, errors);
            return null;
        }
        if (type == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            if (value.isEmpty()) {
                return null;
            }
            if (!INTEGER_TEXT.matcher(value).matches()) {
                addInvalidInteger(cell, column, recordId, sourceFile, sheet, errors);
                return null;
            }
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException exception) {
                addInvalidInteger(cell, column, recordId, sourceFile, sheet, errors);
                return null;
            }
        }
        if (type == CellType.FORMULA) {
            addUnsupportedFormula(cell, column, recordId, sourceFile, sheet, errors);
            return null;
        }
        addInvalidCellType(cell, column, recordId, sourceFile, sheet, "Expected integer numeric or integer text cell.", errors);
        return null;
    }

    String diagnosticValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return formatter.formatCellValue(cell);
    }

    private boolean isBlank(Cell cell) {
        if (cell == null) {
            return true;
        }
        CellType type = effectiveType(cell);
        return type == CellType.BLANK || type == CellType.STRING && cell.getStringCellValue().trim().isEmpty();
    }

    private CellType effectiveType(Cell cell) {
        return Optional.ofNullable(cell).map(Cell::getCellType).orElse(CellType.BLANK);
    }

    private void addInvalidInteger(
            Cell cell,
            ExcelColumn column,
            String recordId,
            String sourceFile,
            String sheet,
            List<ValidationError> errors
    ) {
        errors.add(ExcelValidationErrors.cellError(
                ValidationCode.INVALID_INTEGER_FORMAT,
                ValidationCategory.PARSE,
                "Integer field must contain a whole number.",
                column,
                recordId,
                sourceFile,
                sheet,
                cell.getRowIndex(),
                cell.getColumnIndex(),
                diagnosticValue(cell)
        ));
    }

    private void addUnsupportedFormula(
            Cell cell,
            ExcelColumn column,
            String recordId,
            String sourceFile,
            String sheet,
            List<ValidationError> errors
    ) {
        errors.add(ExcelValidationErrors.cellError(
                ValidationCode.UNSUPPORTED_FORMULA,
                ValidationCategory.PARSE,
                "Formula cells are not supported in Excel input.",
                column,
                recordId,
                sourceFile,
                sheet,
                cell.getRowIndex(),
                cell.getColumnIndex(),
                diagnosticValue(cell)
        ));
    }

    private void addInvalidCellType(
            Cell cell,
            ExcelColumn column,
            String recordId,
            String sourceFile,
            String sheet,
            String message,
            List<ValidationError> errors
    ) {
        errors.add(ExcelValidationErrors.cellError(
                ValidationCode.INVALID_CELL_TYPE,
                ValidationCategory.PARSE,
                message,
                column,
                recordId,
                sourceFile,
                sheet,
                cell.getRowIndex(),
                cell.getColumnIndex(),
                diagnosticValue(cell)
        ));
    }
}
