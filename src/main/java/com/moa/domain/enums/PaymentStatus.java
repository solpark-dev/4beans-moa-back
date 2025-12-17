package com.moa.domain.enums;

public enum PaymentStatus {
    PENDING("결제대기"),
    COMPLETED("결제완료"),
    FAILED("결제실패"),
    REFUNDED("결제환불");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}