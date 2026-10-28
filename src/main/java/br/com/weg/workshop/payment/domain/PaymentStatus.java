package br.com.weg.workshop.payment.domain;

public enum PaymentStatus {
    PENDING,
    PAID,
    DECLINED,
    CANCELLED,
    REFUNDED,
    EXEMPT
}
