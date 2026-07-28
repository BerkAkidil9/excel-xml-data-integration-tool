package com.berk.dataintegration.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineAppTest {
    @TempDir
    Path tempDir;

    private final CommandLineApp app = new CommandLineApp();

    @Test
    void convertsExcelToXmlAndPrintsSuccessSummary() {
        Path output = tempDir.resolve("converted.xml");
        CapturedOutput captured = run(
                "convert",
                "--direction", "excel-to-xml",
                "--input", "examples/valid-invoice-data.xlsx",
                "--output", output.toString()
        );

        assertEquals(CommandLineApp.EXIT_SUCCESS, captured.exitCode());
        assertTrue(Files.exists(output));
        assertTrue(captured.out().contains("Conversion completed."), captured::out);
        assertTrue(captured.out().contains("Records: parties=3, paymentAccounts=2, invoices=2, lines=3"), captured::out);
        assertTrue(captured.out().contains("Output: " + output), captured::out);
        assertTrue(captured.err().isBlank(), captured::err);
    }

    @Test
    void returnsConversionFailureForInvalidInputAndDoesNotWriteOutput() {
        Path output = tempDir.resolve("invalid.xlsx");
        Path report = tempDir.resolve("errors.csv");
        CapturedOutput captured = run(
                "convert",
                "--direction", "xml-to-excel",
                "--input", "src/test/resources/fixtures/xml/invalid-business-wrong-total.xml",
                "--output", output.toString(),
                "--error-report", report.toString()
        );

        assertEquals(CommandLineApp.EXIT_CONVERSION_FAILED, captured.exitCode());
        assertFalse(Files.exists(output));
        assertTrue(Files.exists(report));
        assertTrue(captured.out().contains("Error report: " + report), captured::out);
        assertTrue(captured.err().contains("Conversion failed with 1 error(s)."), captured::err);
        assertTrue(captured.err().contains("XML_INVOICE_TOTAL_MISMATCH"), captured::err);
        assertTrue(captured.err().contains("recordId=INV-001"), captured::err);
    }

    @Test
    void returnsUsageErrorForMissingRequiredOption() {
        CapturedOutput captured = run(
                "convert",
                "--direction", "excel-to-xml",
                "--input", "in.xlsx"
        );

        assertEquals(CommandLineApp.EXIT_USAGE_ERROR, captured.exitCode());
        assertTrue(captured.out().isBlank(), captured::out);
        assertTrue(captured.err().contains("Missing required option: --output"), captured::err);
        assertTrue(captured.err().contains("Usage:"), captured::err);
    }

    @Test
    void printsUsageForHelp() {
        CapturedOutput captured = run("--help");

        assertEquals(CommandLineApp.EXIT_SUCCESS, captured.exitCode());
        assertTrue(captured.out().contains("Usage:"), captured::out);
        assertTrue(captured.out().contains("excel-to-xml|xml-to-excel"), captured::out);
        assertTrue(captured.out().contains("template --output"), captured::out);
        assertTrue(captured.err().isBlank(), captured::err);
    }

    @Test
    void writesExcelTemplate() {
        Path template = tempDir.resolve("invoice-template.xlsx");

        CapturedOutput captured = run("template", "--output", template.toString());

        assertEquals(CommandLineApp.EXIT_SUCCESS, captured.exitCode());
        assertTrue(Files.exists(template));
        assertTrue(captured.out().contains("Template written: " + template), captured::out);
        assertTrue(captured.err().isBlank(), captured::err);
    }

    private CapturedOutput run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = app.run(
                args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8)
        );
        return new CapturedOutput(
                exitCode,
                out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8)
        );
    }

    private record CapturedOutput(
            int exitCode,
            String out,
            String err
    ) {
    }
}
