package com.berk.dataintegration.excel;

import com.berk.dataintegration.validation.ValidationError;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelTemplateWriterTest {
    private final ExcelTemplateWriter writer = new ExcelTemplateWriter();
    private final ExcelWorkbookContractValidator contractValidator = new ExcelWorkbookContractValidator();

    @TempDir
    Path tempDir;

    @Test
    void writesBlankWorkbookWithContractSheetsAndHeaders() throws IOException {
        Path template = tempDir.resolve("invoice-template.xlsx");

        writer.write(template);

        try (InputStream input = Files.newInputStream(template);
             Workbook workbook = WorkbookFactory.create(input)) {
            List<ValidationError> errors = contractValidator.validate(workbook, "invoice-template.xlsx");
            assertTrue(errors.isEmpty(), () -> errors.toString());
            for (ExcelSheetDefinition definition : ExcelSheetDefinition.values()) {
                assertEquals(0, workbook.getSheet(definition.sheetName()).getLastRowNum());
            }
        }
    }
}
