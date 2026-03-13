package com.afsinb.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.math.BigDecimal;

@Slf4j
@Service
public class PaymentService {

    private Map<String, Transaction> transactions = new HashMap<>();
    private List<String> recentErrors = new ArrayList<>();

    public Payment processPayment(PaymentRequest request) {
        try {
            log.info("Processing payment: amount={}, currency={}", request.getAmount(), request.getCurrency());

            // INTENTIONAL BUG #1: NullPointerException if customer is null
            if (request.getCustomer() == null) {
                log.error("Customer object is null");
                throw new NullPointerException("Customer cannot be null");
            }

            // INTENTIONAL BUG #2: Division by zero
            if (request.getExchangeRate() == 0) {
                log.error("Exchange rate is zero - division error");
                // This will cause an error in calculation
                BigDecimal converted = new BigDecimal(request.getAmount()).divide(new BigDecimal(0));
            }

            Payment payment = new Payment();
            payment.setId(UUID.randomUUID().toString());
            payment.setAmount(request.getAmount());
            payment.setStatus("SUCCESS");
            payment.setTimestamp(System.currentTimeMillis());

            log.info("Payment processed successfully: id={}, amount={}", payment.getId(), payment.getAmount());
            return payment;

        } catch (Exception e) {
            log.error("Payment processing failed", e);
            recentErrors.add(e.getMessage());
            if (recentErrors.size() > 100) {
                recentErrors.remove(0);
            }
            throw e;
        }
    }

    public int getErrorCount() {
        return recentErrors.size();
    }

    public double getErrorRate() {
        // Simulate increasing error rate
        return recentErrors.size() * 0.01;
    }
}
