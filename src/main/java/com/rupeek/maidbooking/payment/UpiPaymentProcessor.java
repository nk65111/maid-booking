package com.rupeek.maidbooking.payment;

import org.springframework.stereotype.Component;

import com.rupeek.maidbooking.common.ValidationException;

@Component
public class UpiPaymentProcessor extends GatewayPaymentProcessor {

    public UpiPaymentProcessor(PaymentGateway gateway) {
        super(gateway);
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    protected void validateInstrument(String vpa) {
        if (!vpa.matches("[\\w.\\-]{2,}@[a-zA-Z]{2,}")) {
            throw new ValidationException("invalid UPI id, expected something like name@bank");
        }
    }
}
