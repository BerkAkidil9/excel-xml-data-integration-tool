package com.berk.dataintegration.validation;

import com.berk.dataintegration.TestBatches;
import com.berk.dataintegration.domain.Address;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.domain.Party;
import com.berk.dataintegration.domain.PartyRole;
import com.berk.dataintegration.domain.PaymentAccount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceBatchValidatorTest {
    private final InvoiceBatchValidator validator = new InvoiceBatchValidator();

    @Test
    void validCompleteBatchProducesNoValidationErrors() {
        ValidationResult result = validator.validate(TestBatches.validBatch());

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void missingRequiredFieldReportsCodeFieldAndRecordId() {
        Party supplierWithoutName = new Party(
                "SUP-001",
                PartyRole.SUPPLIER,
                " ",
                null,
                null,
                null,
                new Address(null, "Istanbul", null, "TR")
        );
        InvoiceBatch batch = new InvoiceBatch(
                List.of(supplierWithoutName, TestBatches.customer()),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(TestBatches.invoice("INV-2026-0001", "SUP-001", "CUS-001", "PAY-001", "TRY",
                        List.of(TestBatches.line(1))))
        );

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.REQUIRED_FIELD, "name", "SUP-001");
    }

    @Test
    void duplicatePartyIdReportsCodeFieldAndRecordId() {
        InvoiceBatch batch = new InvoiceBatch(
                List.of(TestBatches.supplier(), TestBatches.customer("SUP-001"), TestBatches.customer()),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(TestBatches.invoice("INV-2026-0001", "SUP-001", "CUS-001", "PAY-001", "TRY",
                        List.of(TestBatches.line(1))))
        );

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.DUPLICATE_PARTY_ID, "partyId", "SUP-001");
    }

    @Test
    void duplicatePaymentAccountIdReportsCodeFieldAndRecordId() {
        InvoiceBatch batch = new InvoiceBatch(
                List.of(TestBatches.supplier(), TestBatches.customer()),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY"), TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(TestBatches.invoice("INV-2026-0001", "SUP-001", "CUS-001", "PAY-001", "TRY",
                        List.of(TestBatches.line(1))))
        );

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.DUPLICATE_PAYMENT_ACCOUNT_ID, "paymentAccountId", "PAY-001");
    }

    @Test
    void duplicateInvoiceNumberReportsCodeFieldAndRecordId() {
        InvoiceBatch batch = new InvoiceBatch(
                List.of(TestBatches.supplier(), TestBatches.customer()),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(
                        TestBatches.invoice("INV-2026-0001", "SUP-001", "CUS-001", "PAY-001", "TRY",
                                List.of(TestBatches.line(1))),
                        TestBatches.invoice("INV-2026-0001", "SUP-001", "CUS-001", "PAY-001", "TRY",
                                List.of(TestBatches.line(1)))
                )
        );

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.DUPLICATE_INVOICE_NUMBER, "invoiceNumber", "INV-2026-0001");
    }

    @Test
    void missingCustomerReferenceReportsCodeFieldAndInvoiceNumber() {
        InvoiceBatch batch = new InvoiceBatch(
                List.of(TestBatches.supplier()),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(TestBatches.invoice("INV-2026-0001", "SUP-001", "MISSING-CUS", "PAY-001", "TRY",
                        List.of(TestBatches.line(1))))
        );

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.MISSING_CUSTOMER_REFERENCE, "customerId", "INV-2026-0001");
    }

    @Test
    void supplierReferencePointingToCustomerRolePartyIsReported() {
        InvoiceBatch batch = new InvoiceBatch(
                List.of(TestBatches.supplier(), TestBatches.customer(), TestBatches.customer("CUS-SUP")),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(TestBatches.invoice("INV-2026-0001", "CUS-SUP", "CUS-001", "PAY-001", "TRY",
                        List.of(TestBatches.line(1))))
        );

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.INVALID_SUPPLIER_ROLE, "supplierId", "INV-2026-0001");
    }

    @Test
    void supplierAndCustomerIdsMustDifferWithinInvoice() {
        Party customerWithSupplierId = new Party(
                "BOTH-001",
                PartyRole.CUSTOMER,
                "Same Party Customer",
                null,
                null,
                null,
                new Address(null, "Ankara", null, "TR")
        );
        InvoiceBatch batch = new InvoiceBatch(
                List.of(customerWithSupplierId),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(TestBatches.invoice("INV-2026-0001", "BOTH-001", "BOTH-001", "PAY-001", "TRY",
                        List.of(TestBatches.line(1))))
        );

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.SUPPLIER_CUSTOMER_SAME, "customerId", "INV-2026-0001");
    }

    @Test
    void dueDateBeforeIssueDateReportsCodeFieldAndInvoiceNumber() {
        Invoice invoice = new Invoice(
                "INV-2026-0001",
                LocalDate.of(2026, 7, 27),
                LocalDate.of(2026, 7, 26),
                "TRY",
                "SUP-001",
                "CUS-001",
                "PAY-001",
                null,
                List.of(TestBatches.line(1))
        );
        InvoiceBatch batch = new InvoiceBatch(
                List.of(TestBatches.supplier(), TestBatches.customer()),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(invoice)
        );

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.DUE_DATE_BEFORE_ISSUE_DATE, "dueDate", "INV-2026-0001");
    }

    @Test
    void zeroQuantityAndNegativeUnitPriceAreBothReported() {
        InvoiceLine line = TestBatches.line(1, "0", "-1.00", "20.00");
        InvoiceBatch batch = batchWithLines(List.of(line));

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.QUANTITY_NOT_POSITIVE, "quantity", "INV-2026-0001#1");
        assertHasError(result, ValidationCode.UNIT_PRICE_NEGATIVE, "unitPrice", "INV-2026-0001#1");
    }

    @Test
    void taxRatesBelowZeroAndAboveOneHundredAreBothReported() {
        InvoiceBatch batch = batchWithLines(List.of(
                TestBatches.line(1, "1", "10.00", "-0.01"),
                TestBatches.line(2, "1", "10.00", "100.01")
        ));

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.TAX_RATE_OUT_OF_RANGE, "taxRate", "INV-2026-0001#1");
        assertHasError(result, ValidationCode.TAX_RATE_OUT_OF_RANGE, "taxRate", "INV-2026-0001#2");
    }

    @Test
    void duplicateLineNumberWithinInvoiceIsReported() {
        InvoiceBatch batch = batchWithLines(List.of(TestBatches.line(1), TestBatches.line(1, "1", "10.00", "20.00")));

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.DUPLICATE_LINE_NUMBER, "lineNumber", "INV-2026-0001#1");
    }

    @Test
    void invoiceWithoutLinesIsReported() {
        InvoiceBatch batch = batchWithLines(List.of());

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.INVOICE_WITHOUT_LINES, "lines", "INV-2026-0001");
    }

    @Test
    void invoiceCurrencyDifferentFromPaymentAccountCurrencyIsReported() {
        InvoiceBatch batch = new InvoiceBatch(
                List.of(TestBatches.supplier(), TestBatches.customer()),
                List.of(TestBatches.paymentAccount("PAY-001", "EUR")),
                List.of(TestBatches.invoice("INV-2026-0001", "SUP-001", "CUS-001", "PAY-001", "TRY",
                        List.of(TestBatches.line(1))))
        );

        ValidationResult result = validator.validate(batch);

        assertHasError(result, ValidationCode.CURRENCY_MISMATCH, "currencyCode", "INV-2026-0001");
    }

    @Test
    void multipleIndependentErrorsAreCollectedTogether() {
        Party invalidSupplier = new Party("SUP-001", PartyRole.SUPPLIER, "", null, "not-an-email", null,
                new Address(null, "", null, "TUR"));
        PaymentAccount duplicateAccount = TestBatches.paymentAccount("PAY-001", "EUR");
        Invoice invoice = new Invoice(
                "INV-2026-0001",
                LocalDate.of(2026, 7, 27),
                LocalDate.of(2026, 7, 26),
                "TRY",
                "SUP-001",
                "MISSING-CUS",
                "PAY-001",
                null,
                List.of(TestBatches.line(1, "0", "-1.00", "101.00"))
        );
        InvoiceBatch batch = new InvoiceBatch(
                List.of(invalidSupplier, TestBatches.supplier()),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY"), duplicateAccount),
                List.of(invoice)
        );

        ValidationResult result = validator.validate(batch);

        assertFalse(result.isValid());
        assertHasError(result, ValidationCode.REQUIRED_FIELD, "name", "SUP-001");
        assertHasError(result, ValidationCode.INVALID_EMAIL, "email", "SUP-001");
        assertHasError(result, ValidationCode.INVALID_COUNTRY_CODE, "address.countryCode", "SUP-001");
        assertHasError(result, ValidationCode.DUPLICATE_PARTY_ID, "partyId", "SUP-001");
        assertHasError(result, ValidationCode.DUPLICATE_PAYMENT_ACCOUNT_ID, "paymentAccountId", "PAY-001");
        assertHasError(result, ValidationCode.DUE_DATE_BEFORE_ISSUE_DATE, "dueDate", "INV-2026-0001");
        assertHasError(result, ValidationCode.MISSING_CUSTOMER_REFERENCE, "customerId", "INV-2026-0001");
        assertHasError(result, ValidationCode.QUANTITY_NOT_POSITIVE, "quantity", "INV-2026-0001#1");
        assertHasError(result, ValidationCode.UNIT_PRICE_NEGATIVE, "unitPrice", "INV-2026-0001#1");
        assertHasError(result, ValidationCode.TAX_RATE_OUT_OF_RANGE, "taxRate", "INV-2026-0001#1");
    }

    private InvoiceBatch batchWithLines(List<InvoiceLine> lines) {
        return new InvoiceBatch(
                List.of(TestBatches.supplier(), TestBatches.customer()),
                List.of(TestBatches.paymentAccount("PAY-001", "TRY")),
                List.of(TestBatches.invoice("INV-2026-0001", "SUP-001", "CUS-001", "PAY-001", "TRY", lines))
        );
    }

    private void assertHasError(ValidationResult result, ValidationCode code, String field, String recordId) {
        assertTrue(
                result.errors().stream().anyMatch(error ->
                        error.code() == code
                                && field.equals(error.field())
                                && recordId.equals(error.recordId())),
                () -> "Expected " + code + " for field " + field + " and record " + recordId
                        + " but errors were " + result.errors()
        );
    }
}
