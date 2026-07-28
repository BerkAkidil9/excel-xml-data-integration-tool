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
                summary
                metric,value
                total_errors,0
                
                summary_by_error
                code,category,field,source_file,sheet_or_path,count,message
                
                details
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
        assertTrue(csv.startsWith("summary\nmetric,value\ntotal_errors,1\n"), csv);
        assertTrue(csv.contains("summary_by_error\ncode,category,field,source_file,sheet_or_path,count,message\n"), csv);
        assertTrue(csv.contains("REQUIRED_FIELD,FIELD,name,input.xlsx,Parties,1,\"Required field is missing, check \"\"name\"\".\""), csv);
        assertTrue(csv.contains("details\ncode,category,field,record_id,source_file,sheet_or_path,row,column,value,message\n"), csv);
        assertTrue(csv.contains("REQUIRED_FIELD,FIELD,name,SUP-001,input.xlsx,Parties,2,C"), csv);
        assertTrue(csv.contains("\"Bad\nValue\""), csv);
        assertTrue(csv.contains("\"Required field is missing, check \"\"name\"\".\""), csv);
    }

    @Test
    void groupsRepeatedErrorsInSummary() throws IOException {
        Path report = tempDir.resolve("errors.csv");
        ValidationError first = ValidationError.of(
                ValidationCode.REQUIRED_FIELD,
                ValidationCategory.FIELD,
                "Missing invoice number.",
                "invoiceNumber",
                "INV-1"
        );
        ValidationError second = ValidationError.of(
                ValidationCode.REQUIRED_FIELD,
                ValidationCategory.FIELD,
                "Missing invoice number.",
                "invoiceNumber",
                "INV-2"
        );

        writer.writeCsv(List.of(first, second), report);

        String csv = Files.readString(report, StandardCharsets.UTF_8);
        assertTrue(csv.contains("total_errors,2"), csv);
        assertTrue(csv.contains("REQUIRED_FIELD,FIELD,invoiceNumber,,,2,Missing invoice number."), csv);
    }
}
