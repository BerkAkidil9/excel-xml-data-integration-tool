package com.berk.dataintegration.validation;

import com.berk.dataintegration.domain.Address;
import com.berk.dataintegration.domain.Invoice;
import com.berk.dataintegration.domain.InvoiceBatch;
import com.berk.dataintegration.domain.InvoiceLine;
import com.berk.dataintegration.domain.Party;
import com.berk.dataintegration.domain.PartyRole;
import com.berk.dataintegration.domain.PaymentAccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class InvoiceBatchValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Z]{2}$");
    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern IBAN_PATTERN = Pattern.compile("^[A-Z0-9]{5,34}$");
    private static final Pattern SWIFT_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{8}([A-Z0-9]{3})?$");
    private static final Pattern UNIT_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{1,8}$");
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public ValidationResult validate(InvoiceBatch batch) {
        List<ValidationError> errors = new ArrayList<>();
        if (batch == null) {
            errors.add(error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD, "Batch is required.", "batch", null));
            return new ValidationResult(errors);
        }

        validateParties(batch.parties(), errors);
        validatePaymentAccounts(batch.paymentAccounts(), errors);
        validateInvoices(batch.invoices(), errors);
        validateReferences(batch, errors);

        return new ValidationResult(errors);
    }

    private void validateParties(List<Party> parties, List<ValidationError> errors) {
        Set<String> partyIds = new HashSet<>();
        for (Party party : parties) {
            String recordId = party == null ? null : party.partyId();
            if (party == null) {
                errors.add(error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD, "Party is required.", "party", null));
                continue;
            }

            validateRequiredText(party.partyId(), "partyId", recordId, 40, errors);
            if (isPresent(party.partyId()) && !partyIds.add(party.partyId())) {
                errors.add(error(ValidationCode.DUPLICATE_PARTY_ID, ValidationCategory.BUSINESS_RULE,
                        "Party ID must be unique.", "partyId", party.partyId()));
            }
            if (party.role() == null) {
                errors.add(error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD,
                        "Party role is required.", "role", recordId));
            }
            validateRequiredText(party.name(), "name", recordId, 200, errors);
            validateOptionalText(party.taxNumber(), "taxNumber", recordId, 30, errors);
            validateOptionalText(party.phone(), "phone", recordId, 30, errors);
            validateOptionalText(party.email(), "email", recordId, 320, errors);
            if (isPresent(party.email()) && !EMAIL_PATTERN.matcher(party.email()).matches()) {
                errors.add(error(ValidationCode.INVALID_EMAIL, ValidationCategory.FIELD,
                        "Email must have a basic address shape.", "email", recordId));
            }
            validateAddress(party.address(), recordId, errors);
        }
    }

    private void validateAddress(Address address, String recordId, List<ValidationError> errors) {
        if (address == null) {
            errors.add(error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD,
                    "Address is required.", "address", recordId));
            return;
        }
        validateOptionalText(address.street(), "address.street", recordId, 250, errors);
        validateRequiredText(address.city(), "address.city", recordId, 100, errors);
        validateOptionalText(address.postalCode(), "address.postalCode", recordId, 20, errors);
        validateRequiredText(address.countryCode(), "address.countryCode", recordId, 2, errors);
        if (isPresent(address.countryCode()) && !COUNTRY_CODE_PATTERN.matcher(address.countryCode()).matches()) {
            errors.add(error(ValidationCode.INVALID_COUNTRY_CODE, ValidationCategory.FIELD,
                    "Country code must be two uppercase letters.", "address.countryCode", recordId));
        }
    }

    private void validatePaymentAccounts(List<PaymentAccount> paymentAccounts, List<ValidationError> errors) {
        Set<String> paymentAccountIds = new HashSet<>();
        for (PaymentAccount account : paymentAccounts) {
            String recordId = account == null ? null : account.paymentAccountId();
            if (account == null) {
                errors.add(error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD,
                        "Payment account is required.", "paymentAccount", null));
                continue;
            }

            validateRequiredText(account.paymentAccountId(), "paymentAccountId", recordId, 40, errors);
            if (isPresent(account.paymentAccountId()) && !paymentAccountIds.add(account.paymentAccountId())) {
                errors.add(error(ValidationCode.DUPLICATE_PAYMENT_ACCOUNT_ID, ValidationCategory.BUSINESS_RULE,
                        "Payment account ID must be unique.", "paymentAccountId", account.paymentAccountId()));
            }
            validateRequiredText(account.accountHolderName(), "accountHolderName", recordId, 200, errors);
            validateRequiredText(account.bankName(), "bankName", recordId, 200, errors);
            validateRequiredText(account.iban(), "iban", recordId, 34, errors);
            if (isPresent(account.iban()) && !IBAN_PATTERN.matcher(account.iban().replace(" ", "")).matches()) {
                errors.add(error(ValidationCode.INVALID_IBAN, ValidationCategory.FIELD,
                        "IBAN must contain 5 to 34 uppercase alphanumeric characters after spaces are removed.",
                        "iban", recordId));
            }
            validateOptionalText(account.swiftCode(), "swiftCode", recordId, 11, errors);
            if (isPresent(account.swiftCode()) && !SWIFT_CODE_PATTERN.matcher(account.swiftCode()).matches()) {
                errors.add(error(ValidationCode.INVALID_SWIFT_CODE, ValidationCategory.FIELD,
                        "SWIFT code must contain 8 or 11 uppercase alphanumeric characters.", "swiftCode", recordId));
            }
            validateRequiredText(account.currencyCode(), "currencyCode", recordId, 3, errors);
            validateCurrencyCode(account.currencyCode(), "currencyCode", recordId, errors);
        }
    }

    private void validateInvoices(List<Invoice> invoices, List<ValidationError> errors) {
        Set<String> invoiceNumbers = new HashSet<>();
        for (Invoice invoice : invoices) {
            String recordId = invoice == null ? null : invoice.invoiceNumber();
            if (invoice == null) {
                errors.add(error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD,
                        "Invoice is required.", "invoice", null));
                continue;
            }

            validateRequiredText(invoice.invoiceNumber(), "invoiceNumber", recordId, 50, errors);
            if (isPresent(invoice.invoiceNumber()) && !invoiceNumbers.add(invoice.invoiceNumber())) {
                errors.add(error(ValidationCode.DUPLICATE_INVOICE_NUMBER, ValidationCategory.BUSINESS_RULE,
                        "Invoice number must be unique.", "invoiceNumber", invoice.invoiceNumber()));
            }
            if (invoice.issueDate() == null) {
                errors.add(error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD,
                        "Issue date is required.", "issueDate", recordId));
            }
            if (invoice.issueDate() != null && invoice.dueDate() != null && invoice.dueDate().isBefore(invoice.issueDate())) {
                errors.add(error(ValidationCode.DUE_DATE_BEFORE_ISSUE_DATE, ValidationCategory.BUSINESS_RULE,
                        "Due date must not be before issue date.", "dueDate", recordId));
            }
            validateRequiredText(invoice.currencyCode(), "currencyCode", recordId, 3, errors);
            validateCurrencyCode(invoice.currencyCode(), "currencyCode", recordId, errors);
            validateRequiredText(invoice.supplierId(), "supplierId", recordId, 40, errors);
            validateRequiredText(invoice.customerId(), "customerId", recordId, 40, errors);
            validateRequiredText(invoice.paymentAccountId(), "paymentAccountId", recordId, 40, errors);
            validateOptionalText(invoice.note(), "note", recordId, 1000, errors);
            validateLines(invoice, errors);
        }
    }

    private void validateLines(Invoice invoice, List<ValidationError> errors) {
        String invoiceRecordId = invoice.invoiceNumber();
        if (invoice.lines().isEmpty()) {
            errors.add(error(ValidationCode.INVOICE_WITHOUT_LINES, ValidationCategory.BUSINESS_RULE,
                    "Invoice must have at least one line.", "lines", invoiceRecordId));
            return;
        }

        Set<Integer> lineNumbers = new HashSet<>();
        for (InvoiceLine line : invoice.lines()) {
            String lineRecordId = invoiceRecordId + "#" + (line == null ? "null" : line.lineNumber());
            if (line == null) {
                errors.add(error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD,
                        "Invoice line is required.", "lines", invoiceRecordId));
                continue;
            }
            if (line.lineNumber() <= 0) {
                errors.add(error(ValidationCode.LINE_NUMBER_NOT_POSITIVE, ValidationCategory.FIELD,
                        "Line number must be positive.", "lineNumber", lineRecordId));
            } else if (!lineNumbers.add(line.lineNumber())) {
                errors.add(error(ValidationCode.DUPLICATE_LINE_NUMBER, ValidationCategory.BUSINESS_RULE,
                        "Line number must be unique within the invoice.", "lineNumber", lineRecordId));
            }
            validateOptionalText(line.itemCode(), "itemCode", lineRecordId, 50, errors);
            validateRequiredText(line.description(), "description", lineRecordId, 500, errors);
            validateRequiredDecimal(line.quantity(), "quantity", lineRecordId, errors);
            if (line.quantity() != null) {
                if (line.quantity().compareTo(ZERO) <= 0) {
                    errors.add(error(ValidationCode.QUANTITY_NOT_POSITIVE, ValidationCategory.FIELD,
                            "Quantity must be greater than zero.", "quantity", lineRecordId));
                }
                validateMaxScale(line.quantity(), 4, "quantity", lineRecordId, errors);
            }
            validateRequiredText(line.unitCode(), "unitCode", lineRecordId, 8, errors);
            if (isPresent(line.unitCode()) && !UNIT_CODE_PATTERN.matcher(line.unitCode()).matches()) {
                errors.add(error(ValidationCode.INVALID_UNIT_CODE, ValidationCategory.FIELD,
                        "Unit code must contain 1 to 8 uppercase letters or digits.", "unitCode", lineRecordId));
            }
            validateRequiredDecimal(line.unitPrice(), "unitPrice", lineRecordId, errors);
            if (line.unitPrice() != null) {
                if (line.unitPrice().compareTo(ZERO) < 0) {
                    errors.add(error(ValidationCode.UNIT_PRICE_NEGATIVE, ValidationCategory.FIELD,
                            "Unit price must be zero or greater.", "unitPrice", lineRecordId));
                }
                validateMaxScale(line.unitPrice(), 4, "unitPrice", lineRecordId, errors);
            }
            validateRequiredDecimal(line.taxRate(), "taxRate", lineRecordId, errors);
            if (line.taxRate() != null) {
                if (line.taxRate().compareTo(ZERO) < 0 || line.taxRate().compareTo(ONE_HUNDRED) > 0) {
                    errors.add(error(ValidationCode.TAX_RATE_OUT_OF_RANGE, ValidationCategory.FIELD,
                            "Tax rate must be between 0 and 100 inclusive.", "taxRate", lineRecordId));
                }
                validateMaxScale(line.taxRate(), 2, "taxRate", lineRecordId, errors);
            }
        }
    }

    private void validateReferences(InvoiceBatch batch, List<ValidationError> errors) {
        Map<String, Party> partiesById = uniquePartiesById(batch.parties());
        Map<String, PaymentAccount> paymentAccountsById = uniquePaymentAccountsById(batch.paymentAccounts());

        for (Invoice invoice : batch.invoices()) {
            if (invoice == null) {
                continue;
            }
            String recordId = invoice.invoiceNumber();
            Party supplier = null;
            Party customer = null;
            PaymentAccount paymentAccount = null;

            if (isPresent(invoice.supplierId())) {
                supplier = partiesById.get(invoice.supplierId());
                if (supplier == null) {
                    errors.add(error(ValidationCode.MISSING_SUPPLIER_REFERENCE, ValidationCategory.REFERENCE,
                            "Supplier party reference must exist.", "supplierId", recordId));
                } else if (supplier.role() != PartyRole.SUPPLIER) {
                    errors.add(error(ValidationCode.INVALID_SUPPLIER_ROLE, ValidationCategory.REFERENCE,
                            "Supplier reference must point to a SUPPLIER party.", "supplierId", recordId));
                }
            }
            if (isPresent(invoice.customerId())) {
                customer = partiesById.get(invoice.customerId());
                if (customer == null) {
                    errors.add(error(ValidationCode.MISSING_CUSTOMER_REFERENCE, ValidationCategory.REFERENCE,
                            "Customer party reference must exist.", "customerId", recordId));
                } else if (customer.role() != PartyRole.CUSTOMER) {
                    errors.add(error(ValidationCode.INVALID_CUSTOMER_ROLE, ValidationCategory.REFERENCE,
                            "Customer reference must point to a CUSTOMER party.", "customerId", recordId));
                }
            }
            if (isPresent(invoice.supplierId()) && invoice.supplierId().equals(invoice.customerId())) {
                errors.add(error(ValidationCode.SUPPLIER_CUSTOMER_SAME, ValidationCategory.BUSINESS_RULE,
                        "Supplier and customer IDs must differ.", "customerId", recordId));
            }
            if (isPresent(invoice.paymentAccountId())) {
                paymentAccount = paymentAccountsById.get(invoice.paymentAccountId());
                if (paymentAccount == null) {
                    errors.add(error(ValidationCode.MISSING_PAYMENT_ACCOUNT_REFERENCE, ValidationCategory.REFERENCE,
                            "Payment account reference must exist.", "paymentAccountId", recordId));
                }
            }
            if (paymentAccount != null
                    && isPresent(invoice.currencyCode())
                    && isPresent(paymentAccount.currencyCode())
                    && !invoice.currencyCode().equals(paymentAccount.currencyCode())) {
                errors.add(error(ValidationCode.CURRENCY_MISMATCH, ValidationCategory.BUSINESS_RULE,
                        "Invoice currency must equal payment account currency.", "currencyCode", recordId));
            }
        }
    }

    private Map<String, Party> uniquePartiesById(List<Party> parties) {
        Map<String, Party> byId = new HashMap<>();
        for (Party party : parties) {
            if (party != null && isPresent(party.partyId())) {
                byId.putIfAbsent(party.partyId(), party);
            }
        }
        return byId;
    }

    private Map<String, PaymentAccount> uniquePaymentAccountsById(List<PaymentAccount> paymentAccounts) {
        Map<String, PaymentAccount> byId = new HashMap<>();
        for (PaymentAccount account : paymentAccounts) {
            if (account != null && isPresent(account.paymentAccountId())) {
                byId.putIfAbsent(account.paymentAccountId(), account);
            }
        }
        return byId;
    }

    private void validateRequiredText(
            String value,
            String field,
            String recordId,
            int maxLength,
            List<ValidationError> errors
    ) {
        if (!isPresent(value)) {
            errors.add(error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD,
                    "Required field is missing.", field, recordId));
            return;
        }
        if (value.length() > maxLength) {
            errors.add(error(ValidationCode.FIELD_TOO_LONG, ValidationCategory.FIELD,
                    "Field exceeds maximum length " + maxLength + ".", field, recordId));
        }
    }

    private void validateOptionalText(
            String value,
            String field,
            String recordId,
            int maxLength,
            List<ValidationError> errors
    ) {
        if (isPresent(value) && value.length() > maxLength) {
            errors.add(error(ValidationCode.FIELD_TOO_LONG, ValidationCategory.FIELD,
                    "Field exceeds maximum length " + maxLength + ".", field, recordId));
        }
    }

    private void validateRequiredDecimal(
            BigDecimal value,
            String field,
            String recordId,
            List<ValidationError> errors
    ) {
        if (value == null) {
            errors.add(error(ValidationCode.REQUIRED_FIELD, ValidationCategory.FIELD,
                    "Required decimal field is missing.", field, recordId));
        }
    }

    private void validateMaxScale(
            BigDecimal value,
            int maxScale,
            String field,
            String recordId,
            List<ValidationError> errors
    ) {
        int effectiveScale = Math.max(0, value.stripTrailingZeros().scale());
        if (effectiveScale > maxScale) {
            errors.add(error(ValidationCode.DECIMAL_SCALE_EXCEEDED, ValidationCategory.FIELD,
                    "Decimal field exceeds maximum scale " + maxScale + ".", field, recordId));
        }
    }

    private void validateCurrencyCode(
            String value,
            String field,
            String recordId,
            List<ValidationError> errors
    ) {
        if (isPresent(value) && !CURRENCY_CODE_PATTERN.matcher(value).matches()) {
            errors.add(error(ValidationCode.INVALID_CURRENCY_CODE, ValidationCategory.FIELD,
                    "Currency code must be three uppercase letters.", field, recordId));
        }
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private ValidationError error(
            ValidationCode code,
            ValidationCategory category,
            String message,
            String field,
            String recordId
    ) {
        return ValidationError.of(code, category, message, field, recordId);
    }
}
