package com.rupeek.maidbooking.payment;

import org.springframework.stereotype.Component;

import com.rupeek.maidbooking.common.ValidationException;

@Component
public class CardPaymentProcessor extends GatewayPaymentProcessor {

    public CardPaymentProcessor(PaymentGateway gateway) {
        super(gateway);
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    protected void validateInstrument(String cardNumber) {
        if (!cardNumber.matches("\\d{12,19}")) {
            throw new ValidationException("card number must be 12 to 19 digits");
        }
    }
}
