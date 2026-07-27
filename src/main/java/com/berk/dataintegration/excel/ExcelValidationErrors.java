package com.berk.dataintegration.excel;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellReference;

final class ExcelValidationErrors {
    private ExcelValidationErrors() {
    }

    static ValidationError error(
            ValidationCode code,
            ValidationCategory category,
            String message,
            String field,
            String recordId,
            String sourceFile,
            String sheet,
            Integer row,
            String column,
            String value
    ) {
        return new ValidationError(code, category, message, field, recordId, sourceFile, sheet, row, column, value);
    }

    static ValidationError cellError(
            ValidationCode code,
            ValidationCategory category,
            String message,
            ExcelColumn column,
            String recordId,
            String sourceFile,
            String sheet,
            int rowIndex,
            int columnIndex,
            String value
    ) {
        return error(
                code,
                category,
                message,
                column == null ? null : column.field(),
                recordId,
                sourceFile,
                sheet,
                rowIndex + 1,
                CellReference.convertNumToColString(columnIndex),
                value
        );
    }

    static ExcelSourceLocation sourceLocation(
            String sourceFile,
            String sheet,
            int rowIndex,
            int columnIndex,
            ExcelColumn column,
            String value
    ) {
        return new ExcelSourceLocation(
                sourceFile,
                sheet,
                rowIndex + 1,
                CellReference.convertNumToColString(columnIndex),
                column.field(),
                value
        );
    }

    static String cellAddress(Cell cell) {
        if (cell == null) {
            return null;
        }
        return CellReference.convertNumToColString(cell.getColumnIndex()) + (cell.getRowIndex() + 1);
    }
}
