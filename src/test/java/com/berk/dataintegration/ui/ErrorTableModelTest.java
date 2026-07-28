package com.berk.dataintegration.ui;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErrorTableModelTest {
    @Test
    void exposesStableColumnsAndStructuredValues() {
        ErrorTableModel model = new ErrorTableModel();
        ValidationError error = error(ValidationCategory.FIELD);

        model.setErrors(List.of(error));

        assertEquals(1, model.getRowCount());
        assertEquals(10, model.getColumnCount());
        assertEquals("Code", model.getColumnName(0));
        assertEquals("Category", model.getColumnName(1));
        assertEquals("Message", model.getColumnName(9));
        assertEquals(ValidationCode.REQUIRED_FIELD, model.getValueAt(0, 0));
        assertEquals(ValidationCategory.FIELD, model.getValueAt(0, 1));
        assertEquals("invoiceNumber", model.getValueAt(0, 2));
        assertEquals("INV-1", model.getValueAt(0, 3));
        assertEquals("input.xlsx", model.getValueAt(0, 4));
        assertEquals("Invoices", model.getValueAt(0, 5));
        assertEquals(2, model.getValueAt(0, 6));
        assertEquals("A", model.getValueAt(0, 7));
        assertEquals("bad", model.getValueAt(0, 8));
        assertEquals("Missing.", model.getValueAt(0, 9));
        assertEquals(error, model.errorAt(0));
    }

    @Test
    void protectsErrorsFromExternalMutationAndHandlesOutOfRangeRows() {
        ErrorTableModel model = new ErrorTableModel();
        List<ValidationError> errors = new ArrayList<>();
        errors.add(error(ValidationCategory.FIELD));

        model.setErrors(errors);
        errors.clear();

        assertEquals(1, model.getRowCount());
        assertNull(model.errorAt(-1));
        assertNull(model.errorAt(1));
    }

    private ValidationError error(ValidationCategory category) {
        return new ValidationError(
                ValidationCode.REQUIRED_FIELD,
                category,
                "Missing.",
                "invoiceNumber",
                "INV-1",
                "input.xlsx",
                "Invoices",
                2,
                "A",
                "bad"
        );
    }
}
