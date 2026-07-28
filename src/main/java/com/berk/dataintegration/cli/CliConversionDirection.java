package com.berk.dataintegration.cli;

enum CliConversionDirection {
    EXCEL_TO_XML("excel-to-xml"),
    XML_TO_EXCEL("xml-to-excel");

    private final String option;

    CliConversionDirection(String option) {
        this.option = option;
    }

    String option() {
        return option;
    }

    static CliConversionDirection fromOption(String option) {
        for (CliConversionDirection direction : values()) {
            if (direction.option.equals(option)) {
                return direction;
            }
        }
        return null;
    }
}
