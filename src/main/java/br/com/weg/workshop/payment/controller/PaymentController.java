package br.com.weg.workshop.payment.controller;

import br.com.weg.workshop.payment.dto.PaymentResponse;
import br.com.weg.workshop.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping("/api/v1/registrations/{registrationId}/payments")
    @Operation(summary = "Create an idempotent simulated payment")
    public ResponseEntity<PaymentResponse> create(@PathVariable UUID registrationId,
                                                   @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                                   Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(UUID.fromString(authentication.getName()), registrationId, idempotencyKey));
    }

    @PatchMapping("/api/v1/payments/{paymentId}/simulate/paid")
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    @Operation(summary = "Simulate a successful payment callback")
    public PaymentResponse simulatePaid(@PathVariable UUID paymentId) { return service.simulatePaid(paymentId); }

    @PatchMapping("/api/v1/payments/{paymentId}/simulate/declined")
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    @Operation(summary = "Simulate a declined payment callback")
    public PaymentResponse simulateDeclined(@PathVariable UUID paymentId) { return service.simulateDeclined(paymentId); }
}
