package com.rupeek.maidbooking.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Approves everything except instruments ending in "0000", so the failure path can be tried
 * from Postman/curl (e.g. card 4111111111110000).
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResponse charge(PaymentMethod method, String instrument, BigDecimal amount, String reference) {
        if (instrument.endsWith("0000")) {
            return new GatewayResponse(false, null, "Declined by issuer");
        }
        return new GatewayResponse(true, "txn_" + UUID.randomUUID(), "Approved");
    }

    @Override
    public GatewayResponse refund(String chargeReference, BigDecimal amount) {
        return new GatewayResponse(true, "rfnd_" + UUID.randomUUID(), "Refunded");
    }
}
