package com.rupeek.maidbooking.payment;

import java.math.BigDecimal;

import com.rupeek.maidbooking.common.Checks;

/**
 * The methods differ only in how the instrument (card number, VPA, wallet number) is validated;
 * the actual money movement goes through the same gateway.
 */
public abstract class GatewayPaymentProcessor implements PaymentProcessor {

    private final PaymentGateway gateway;

    protected GatewayPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    protected abstract void validateInstrument(String instrument);

    @Override
    public GatewayResponse charge(String instrument, BigDecimal amount, String reference) {
        String cleaned = Checks.requireText(instrument, "instrument");
        validateInstrument(cleaned);
        return gateway.charge(method(), cleaned, amount, reference);
    }

    @Override
    public GatewayResponse refund(String chargeReference, BigDecimal amount) {
        return gateway.refund(chargeReference, amount);
    }
}
