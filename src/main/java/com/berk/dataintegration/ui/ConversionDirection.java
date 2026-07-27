package com.berk.dataintegration.ui;

public enum ConversionDirection {
    EXCEL_TO_XML("Excel to XML", "xlsx", "xml"),
    XML_TO_EXCEL("XML to Excel", "xml", "xlsx");

    private final String label;
    private final String inputExtension;
    private final String outputExtension;

    ConversionDirection(String label, String inputExtension, String outputExtension) {
        this.label = label;
        this.inputExtension = inputExtension;
        this.outputExtension = outputExtension;
    }

    public String inputExtension() {
        return inputExtension;
    }

    public String outputExtension() {
        return outputExtension;
    }

    @Override
    public String toString() {
        return label;
    }
}
