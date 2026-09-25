package br.com.weg.workshop.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {
    String createPayment(BigDecimal amount);
    void cancelPayment(String externalReference);
    void refundPayment(String externalReference, BigDecimal amount);
}
