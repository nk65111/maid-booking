package com.rupeek.maidbooking.payment;

public record GatewayResponse(boolean success, String reference, String message) {
}
