package com.berk.dataintegration.cli;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

record CliArguments(
        CliCommand command,
        CliConversionDirection direction,
        Path inputPath,
        Path outputPath,
        Path errorReportPath,
        boolean help,
        String errorMessage
) {
    static CliArguments parse(String[] args) {
        if (args == null || args.length == 0) {
            return helpArguments();
        }
        if (args.length == 1 && isHelp(args[0])) {
            return helpArguments();
        }
        String command = args[0];
        Map<String, String> options = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            String option = args[i];
            if (isHelp(option)) {
                return helpArguments();
            }
            if (!option.startsWith("--")) {
                return error("Unexpected argument: " + option);
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                return error("Missing value for option: " + option);
            }
            if (options.put(option, args[++i]) != null) {
                return error("Duplicate option: " + option);
            }
        }

        if ("template".equals(command)) {
            return parseTemplate(options);
        }
        if (!"convert".equals(command)) {
            return error("Unknown command: " + command);
        }
        return parseConvert(options);
    }

    private static CliArguments parseConvert(Map<String, String> options) {
        String directionValue = options.remove("--direction");
        String inputValue = options.remove("--input");
        String outputValue = options.remove("--output");
        String errorReportValue = options.remove("--error-report");
        if (!options.isEmpty()) {
            return error("Unknown option: " + options.keySet().iterator().next());
        }
        if (directionValue == null) {
            return error("Missing required option: --direction");
        }
        if (inputValue == null) {
            return error("Missing required option: --input");
        }
        if (outputValue == null) {
            return error("Missing required option: --output");
        }

        CliConversionDirection direction = CliConversionDirection.fromOption(directionValue);
        if (direction == null) {
            return error("Unsupported direction: " + directionValue);
        }
        return new CliArguments(
                CliCommand.CONVERT,
                direction,
                Path.of(inputValue),
                Path.of(outputValue),
                errorReportValue == null ? null : Path.of(errorReportValue),
                false,
                null
        );
    }

    private static CliArguments parseTemplate(Map<String, String> options) {
        String outputValue = options.remove("--output");
        if (!options.isEmpty()) {
            return error("Unknown option: " + options.keySet().iterator().next());
        }
        if (outputValue == null) {
            return error("Missing required option: --output");
        }
        return new CliArguments(CliCommand.TEMPLATE, null, null, Path.of(outputValue), null, false, null);
    }

    private static boolean isHelp(String value) {
        return "--help".equals(value) || "-h".equals(value);
    }

    private static CliArguments helpArguments() {
        return new CliArguments(null, null, null, null, null, true, null);
    }

    private static CliArguments error(String message) {
        return new CliArguments(null, null, null, null, null, false, message);
    }
}
