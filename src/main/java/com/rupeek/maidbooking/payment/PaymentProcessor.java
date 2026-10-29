package com.rupeek.maidbooking.payment;

import java.math.BigDecimal;

/**
 * One implementation per payment method. Adding e.g. net-banking = new enum value + new
 * processor bean; PaymentService picks it up automatically.
 */
public interface PaymentProcessor {

    PaymentMethod method();

    GatewayResponse charge(String instrument, BigDecimal amount, String reference);

    GatewayResponse refund(String chargeReference, BigDecimal amount);
}
