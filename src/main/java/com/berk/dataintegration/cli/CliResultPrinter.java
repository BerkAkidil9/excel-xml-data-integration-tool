package com.berk.dataintegration.cli;

import com.berk.dataintegration.service.ConversionRecordCounts;
import com.berk.dataintegration.service.ConversionResult;
import com.berk.dataintegration.validation.ValidationError;

import java.io.PrintStream;

final class CliResultPrinter {
    private CliResultPrinter() {
    }

    static void printSuccess(ConversionResult result, PrintStream out) {
        out.println("Conversion completed.");
        printCounts(result.recordCounts(), out);
        out.println("Output: " + result.outputPath());
    }

    static void printFailure(ConversionResult result, PrintStream err) {
        err.println("Conversion failed with " + result.errors().size() + " error(s).");
        printCounts(result.recordCounts(), err);
        for (ValidationError error : result.errors()) {
            printError(error, err);
        }
    }

    static void printUsage(PrintStream out) {
        out.println("Usage:");
        out.println("  java -jar excel-xml-data-integration-tool.jar convert --direction <excel-to-xml|xml-to-excel> --input <path> --output <path> [--error-report <csv-path>]");
        out.println("  java -jar excel-xml-data-integration-tool.jar template --output <xlsx-path>");
    }

    private static void printCounts(ConversionRecordCounts counts, PrintStream stream) {
        stream.println("Records: parties=" + counts.partyCount()
                + ", paymentAccounts=" + counts.paymentAccountCount()
                + ", invoices=" + counts.invoiceCount()
                + ", lines=" + counts.invoiceLineCount());
    }

    private static void printError(ValidationError error, PrintStream err) {
        err.println("- code=" + value(error.code())
                + ", category=" + value(error.category())
                + ", source=" + value(error.sourceFile())
                + ", sheetOrPath=" + value(error.sheet())
                + ", row=" + value(error.row())
                + ", column=" + value(error.column())
                + ", field=" + value(error.field())
                + ", recordId=" + value(error.recordId())
                + ", value=" + value(error.value())
                + ", message=" + value(error.message()));
    }

    private static String value(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains("\n") || text.contains("\r")) {
            return text.replace("\r", "\\r").replace("\n", "\\n");
        }
        return text;
    }
}
