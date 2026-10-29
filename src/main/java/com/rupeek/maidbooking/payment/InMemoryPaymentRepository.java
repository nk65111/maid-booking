package com.rupeek.maidbooking.payment;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final Map<String, String> idsByIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        payments.put(payment.getId(), payment);
        idsByIdempotencyKey.put(payment.getIdempotencyKey(), payment.getId());
        return payment;
    }

    @Override
    public Optional<Payment> findById(String id) {
        return Optional.ofNullable(payments.get(id));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(idsByIdempotencyKey.get(idempotencyKey)).map(payments::get);
    }
}
