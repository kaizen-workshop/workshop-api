package br.com.weg.workshop.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {
    @Override public String createPayment(BigDecimal amount) { return "simulated-" + UUID.randomUUID(); }
    @Override public void cancelPayment(String externalReference) { }
    @Override public void refundPayment(String externalReference, BigDecimal amount) { }
}
