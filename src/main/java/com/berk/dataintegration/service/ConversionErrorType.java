package com.berk.dataintegration.service;

import com.berk.dataintegration.validation.ValidationCategory;
import com.berk.dataintegration.validation.ValidationCode;
import com.berk.dataintegration.validation.ValidationError;

public enum ConversionErrorType {
    PARSE,
    CONTRACT,
    XSD,
    DOMAIN_VALIDATION,
    BUSINESS_VALIDATION,
    IO;

    static ConversionErrorType from(ValidationError error) {
        if (error.code() == ValidationCode.XML_SCHEMA_VALIDATION_FAILED) {
            return XSD;
        }
        if (error.category() == ValidationCategory.IO) {
            return IO;
        }
        if (error.category() == ValidationCategory.PARSE) {
            return PARSE;
        }
        if (error.category() == ValidationCategory.CONTRACT) {
            return CONTRACT;
        }
        if (error.category() == ValidationCategory.BUSINESS_RULE) {
            return BUSINESS_VALIDATION;
        }
        return DOMAIN_VALIDATION;
    }
}
