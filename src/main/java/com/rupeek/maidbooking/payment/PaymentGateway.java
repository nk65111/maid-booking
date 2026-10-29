package com.rupeek.maidbooking.payment;

import java.math.BigDecimal;

/**
 * Our own seam over the third-party gateway (Razorpay, Juspay, ...).
 */
public interface PaymentGateway {

    GatewayResponse charge(PaymentMethod method, String instrument, BigDecimal amount, String reference);

    GatewayResponse refund(String chargeReference, BigDecimal amount);
}
