package com.rupeek.maidbooking.payment;

import org.springframework.stereotype.Component;

import com.rupeek.maidbooking.common.ValidationException;

@Component
public class WalletPaymentProcessor extends GatewayPaymentProcessor {

    public WalletPaymentProcessor(PaymentGateway gateway) {
        super(gateway);
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.WALLET;
    }

    @Override
    protected void validateInstrument(String walletNumber) {
        if (!walletNumber.matches("\\d{10}")) {
            throw new ValidationException("wallet number must be the 10 digit mobile number");
        }
    }
}
