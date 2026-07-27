package com.berk.dataintegration.excel;

import java.util.List;
import java.util.Optional;

enum ExcelSheetDefinition {
    PARTIES("Parties", List.of(
            new ExcelColumn("party_id", "partyId", ExcelCellKind.TEXT),
            new ExcelColumn("party_role", "role", ExcelCellKind.PARTY_ROLE),
            new ExcelColumn("name", "name", ExcelCellKind.TEXT),
            new ExcelColumn("tax_number", "taxNumber", ExcelCellKind.TEXT),
            new ExcelColumn("email", "email", ExcelCellKind.TEXT),
            new ExcelColumn("phone", "phone", ExcelCellKind.TEXT),
            new ExcelColumn("street", "address.street", ExcelCellKind.TEXT),
            new ExcelColumn("city", "address.city", ExcelCellKind.TEXT),
            new ExcelColumn("postal_code", "address.postalCode", ExcelCellKind.TEXT),
            new ExcelColumn("country_code", "address.countryCode", ExcelCellKind.TEXT)
    )),
    PAYMENT_ACCOUNTS("PaymentAccounts", List.of(
            new ExcelColumn("payment_account_id", "paymentAccountId", ExcelCellKind.TEXT),
            new ExcelColumn("account_holder_name", "accountHolderName", ExcelCellKind.TEXT),
            new ExcelColumn("bank_name", "bankName", ExcelCellKind.TEXT),
            new ExcelColumn("iban", "iban", ExcelCellKind.TEXT),
            new ExcelColumn("swift_code", "swiftCode", ExcelCellKind.TEXT),
            new ExcelColumn("currency_code", "currencyCode", ExcelCellKind.TEXT)
    )),
    INVOICES("Invoices", List.of(
            new ExcelColumn("invoice_number", "invoiceNumber", ExcelCellKind.TEXT),
            new ExcelColumn("issue_date", "issueDate", ExcelCellKind.DATE),
            new ExcelColumn("due_date", "dueDate", ExcelCellKind.DATE),
            new ExcelColumn("currency_code", "currencyCode", ExcelCellKind.TEXT),
            new ExcelColumn("supplier_id", "supplierId", ExcelCellKind.TEXT),
            new ExcelColumn("customer_id", "customerId", ExcelCellKind.TEXT),
            new ExcelColumn("payment_account_id", "paymentAccountId", ExcelCellKind.TEXT),
            new ExcelColumn("note", "note", ExcelCellKind.TEXT)
    )),
    INVOICE_LINES("InvoiceLines", List.of(
            new ExcelColumn("invoice_number", "invoiceNumber", ExcelCellKind.TEXT),
            new ExcelColumn("line_number", "lineNumber", ExcelCellKind.INTEGER),
            new ExcelColumn("item_code", "itemCode", ExcelCellKind.TEXT),
            new ExcelColumn("description", "description", ExcelCellKind.TEXT),
            new ExcelColumn("quantity", "quantity", ExcelCellKind.DECIMAL),
            new ExcelColumn("unit_code", "unitCode", ExcelCellKind.TEXT),
            new ExcelColumn("unit_price", "unitPrice", ExcelCellKind.DECIMAL),
            new ExcelColumn("tax_rate", "taxRate", ExcelCellKind.DECIMAL)
    ));

    private final String sheetName;
    private final List<ExcelColumn> columns;

    ExcelSheetDefinition(String sheetName, List<ExcelColumn> columns) {
        this.sheetName = sheetName;
        this.columns = columns;
    }

    String sheetName() {
        return sheetName;
    }

    List<ExcelColumn> columns() {
        return columns;
    }

    ExcelColumn column(int index) {
        return columns.get(index);
    }

    int columnCount() {
        return columns.size();
    }

    static Optional<ExcelSheetDefinition> bySheetName(String sheetName) {
        for (ExcelSheetDefinition definition : values()) {
            if (definition.sheetName.equals(sheetName)) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }
}
