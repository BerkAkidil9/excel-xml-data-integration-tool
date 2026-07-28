package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionRecordCounts;
import com.berk.dataintegration.validation.ValidationError;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ConversionViewModel {
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private ConversionDirection direction = ConversionDirection.EXCEL_TO_XML;
    private Path inputPath;
    private Path outputPath;
    private boolean running;
    private String status = "Ready.";
    private ConversionStatusSeverity statusSeverity = ConversionStatusSeverity.INFO;
    private ConversionRecordCounts recordCounts = ConversionRecordCounts.empty();
    private List<ValidationError> errors = List.of();

    public ConversionDirection direction() {
        return direction;
    }

    public void setDirection(ConversionDirection direction) {
        setProperty("direction", this.direction, this.direction = Objects.requireNonNull(direction));
    }

    public Path inputPath() {
        return inputPath;
    }

    public void setInputPath(Path inputPath) {
        setProperty("inputPath", this.inputPath, this.inputPath = inputPath);
    }

    public Path outputPath() {
        return outputPath;
    }

    public void setOutputPath(Path outputPath) {
        setProperty("outputPath", this.outputPath, this.outputPath = outputPath);
    }

    public boolean running() {
        return running;
    }

    public void setRunning(boolean running) {
        setProperty("running", this.running, this.running = running);
    }

    public String status() {
        return status;
    }

    public void setStatus(String status) {
        setStatus(status, ConversionStatusSeverity.INFO);
    }

    public ConversionStatusSeverity statusSeverity() {
        return statusSeverity;
    }

    public void setStatus(String status, ConversionStatusSeverity severity) {
        setProperty("status", this.status, this.status = status == null ? "" : status);
        ConversionStatusSeverity nextSeverity = severity == null ? ConversionStatusSeverity.INFO : severity;
        setProperty("statusSeverity", this.statusSeverity, this.statusSeverity = nextSeverity);
    }

    public ConversionRecordCounts recordCounts() {
        return recordCounts;
    }

    public void setRecordCounts(ConversionRecordCounts recordCounts) {
        ConversionRecordCounts next = recordCounts == null ? ConversionRecordCounts.empty() : recordCounts;
        setProperty("recordCounts", this.recordCounts, this.recordCounts = next);
    }

    public List<ValidationError> errors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        List<ValidationError> next = errors == null ? List.of() : List.copyOf(errors);
        setProperty("errors", this.errors, this.errors = next);
    }

    public ConversionRequest request() {
        return new ConversionRequest(direction, inputPath, outputPath);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    private void setProperty(String propertyName, Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.firePropertyChange(propertyName, oldValue, newValue);
        }
    }
}
