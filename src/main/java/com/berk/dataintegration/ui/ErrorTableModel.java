package com.berk.dataintegration.ui;

import com.berk.dataintegration.validation.ValidationError;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

final class ErrorTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "Code", "Category", "Field", "Record", "Source", "Sheet/Path", "Row", "Column", "Value", "Message"
    };
    private List<ValidationError> errors = new ArrayList<>();

    void setErrors(List<ValidationError> errors) {
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        fireTableDataChanged();
    }

    ValidationError errorAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= errors.size()) {
            return null;
        }
        return errors.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return errors.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ValidationError error = errors.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> error.code();
            case 1 -> error.category();
            case 2 -> error.field();
            case 3 -> error.recordId();
            case 4 -> error.sourceFile();
            case 5 -> error.sheet();
            case 6 -> error.row();
            case 7 -> error.column();
            case 8 -> error.value();
            case 9 -> error.message();
            default -> "";
        };
    }
}
