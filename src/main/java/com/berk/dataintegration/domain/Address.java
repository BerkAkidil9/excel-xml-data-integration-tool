package com.berk.dataintegration.domain;

public record Address(
        String street,
        String city,
        String postalCode,
        String countryCode
) {
    public Address {
        street = DomainText.optional(street);
        city = DomainText.trim(city);
        postalCode = DomainText.optional(postalCode);
        countryCode = DomainText.trim(countryCode);
    }
}
