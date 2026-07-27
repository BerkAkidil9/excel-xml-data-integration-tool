package com.berk.dataintegration.domain;

public record Party(
        String partyId,
        PartyRole role,
        String name,
        String taxNumber,
        String email,
        String phone,
        Address address
) {
    public Party {
        partyId = DomainText.trim(partyId);
        name = DomainText.trim(name);
        taxNumber = DomainText.optional(taxNumber);
        email = DomainText.optional(email);
        phone = DomainText.optional(phone);
    }
}
