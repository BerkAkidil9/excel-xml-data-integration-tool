package com.berk.dataintegration.cli;

import com.berk.dataintegration.excel.ExcelTemplateWriter;
import com.berk.dataintegration.service.ConversionErrorReportWriter;
import com.berk.dataintegration.service.ConversionResult;
import com.berk.dataintegration.service.InvoiceConversionService;

import java.io.IOException;
import java.io.PrintStream;

public final class CommandLineApp {
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_CONVERSION_FAILED = 1;
    public static final int EXIT_USAGE_ERROR = 2;

    private final InvoiceConversionService conversionService;
    private final ConversionErrorReportWriter errorReportWriter;
    private final ExcelTemplateWriter templateWriter;

    public CommandLineApp() {
        this(new InvoiceConversionService(), new ConversionErrorReportWriter(), new ExcelTemplateWriter());
    }

    CommandLineApp(
            InvoiceConversionService conversionService,
            ConversionErrorReportWriter errorReportWriter,
            ExcelTemplateWriter templateWriter
    ) {
        this.conversionService = conversionService;
        this.errorReportWriter = errorReportWriter;
        this.templateWriter = templateWriter;
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        CliArguments arguments = CliArguments.parse(args);
        if (arguments.help()) {
            CliResultPrinter.printUsage(out);
            return EXIT_SUCCESS;
        }
        if (arguments.errorMessage() != null) {
            err.println(arguments.errorMessage());
            CliResultPrinter.printUsage(err);
            return EXIT_USAGE_ERROR;
        }

        if (arguments.command() == CliCommand.TEMPLATE) {
            return writeTemplate(arguments, out, err);
        }

        ConversionResult result = convert(arguments);
        if (result.isSuccess()) {
            CliResultPrinter.printSuccess(result, out);
            return writeErrorReport(arguments, result, out, err) ? EXIT_SUCCESS : EXIT_CONVERSION_FAILED;
        }
        CliResultPrinter.printFailure(result, err);
        writeErrorReport(arguments, result, out, err);
        return EXIT_CONVERSION_FAILED;
    }

    private ConversionResult convert(CliArguments arguments) {
        return switch (arguments.direction()) {
            case EXCEL_TO_XML -> conversionService.convertExcelToXml(arguments.inputPath(), arguments.outputPath());
            case XML_TO_EXCEL -> conversionService.convertXmlToExcel(arguments.inputPath(), arguments.outputPath());
        };
    }

    private int writeTemplate(CliArguments arguments, PrintStream out, PrintStream err) {
        try {
            templateWriter.write(arguments.outputPath());
            out.println("Template written: " + arguments.outputPath());
            return EXIT_SUCCESS;
        } catch (IOException | RuntimeException exception) {
            err.println("Template could not be written: " + exception.getMessage());
            return EXIT_CONVERSION_FAILED;
        }
    }

    private boolean writeErrorReport(
            CliArguments arguments,
            ConversionResult result,
            PrintStream out,
            PrintStream err
    ) {
        if (arguments.errorReportPath() == null) {
            return true;
        }
        try {
            errorReportWriter.writeCsv(result.errors(), arguments.errorReportPath());
            out.println("Error report: " + arguments.errorReportPath());
            return true;
        } catch (IOException exception) {
            err.println("Error report could not be written: " + exception.getMessage());
            return false;
        }
    }
}
