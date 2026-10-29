package com.rupeek.maidbooking.payment;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/bookings/{bookingId}/payments")
    public Payment pay(@PathVariable String bookingId,
                       @RequestHeader("Idempotency-Key") String idempotencyKey,
                       @RequestBody PayRequest request) {
        return paymentService.pay(bookingId, request, idempotencyKey);
    }
}
