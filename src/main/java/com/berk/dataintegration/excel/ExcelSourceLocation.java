package com.berk.dataintegration.excel;

record ExcelSourceLocation(
        String sourceFile,
        String sheet,
        int row,
        String column,
        String field,
        String value
) {
}
