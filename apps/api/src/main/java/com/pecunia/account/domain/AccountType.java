package com.pecunia.account.domain;

public enum AccountType {
    CURRENT,
    SAVINGS,
    CREDIT_CARD;

    public boolean requiresIban() {
        return this != AccountType.CREDIT_CARD;
    }
}
