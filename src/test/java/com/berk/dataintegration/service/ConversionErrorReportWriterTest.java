package com.berk.dataintegration.service;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversionErrorReportWriterTest {
    private final ConversionErrorReportWriter writer = new ConversionErrorReportWriter();

    @TempDir
    Path tempDir;

    @Test
    void writesHeaderForEmptyErrorList() throws IOException {
        Path report = tempDir.resolve("errors.csv");

        writer.writeCsv(List.of(), report);

        assertEquals("""
                code,category,field,record_id,source_file,sheet_or_path,row,column,value,message
                """, Files.readString(report, StandardCharsets.UTF_8));
    }

    @Test
    void writesStructuredErrorsAndEscapesCsvValues() throws IOException {
        Path report = tempDir.resolve("nested").resolve("errors.csv");
        ValidationError error = new ValidationError(
                ValidationCode.REQUIRED_FIELD,
                ValidationCategory.FIELD,
                "Required field is missing, check \"name\".",
                "name",
                "SUP-001",
                "input.xlsx",
                "Parties",
                2,
                "C",
                "Bad\nValue"
        );

        writer.writeCsv(List.of(error), report);

        String csv = Files.readString(report, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("code,category,field,record_id,source_file,sheet_or_path,row,column,value,message\n"), csv);
        assertTrue(csv.contains("REQUIRED_FIELD,FIELD,name,SUP-001,input.xlsx,Parties,2,C"), csv);
        assertTrue(csv.contains("\"Bad\nValue\""), csv);
        assertTrue(csv.contains("\"Required field is missing, check \"\"name\"\".\""), csv);
    }
}
