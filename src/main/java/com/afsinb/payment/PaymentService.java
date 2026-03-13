package com.afsinb.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    // Dynamic anomaly toggles for live demos
    private volatile boolean nullCustomerFailureEnabled = true;
    private volatile boolean divisionByZeroEnabled = true;
    private volatile int forcedFailuresRemaining = 0;

    private final List<String> recentErrors = new ArrayList<>();

    public Payment processPayment(PaymentRequest request) {
        try {
            log.info("Processing payment: amount={}, currency={}", request.getAmount(), request.getCurrency());

            if (forcedFailuresRemaining > 0) {
                forcedFailuresRemaining--;
                log.error("Forced payment failure injected by anomaly toggle. Remaining={}", forcedFailuresRemaining);
                throw new IllegalStateException("Injected payment failure");
            }

            if (request.getCustomer() == null) {
                if (nullCustomerFailureEnabled) {
                    log.error("Customer object is null");
                    throw new NullPointerException("Customer cannot be null");
                }
                log.warn("[UAC Fix] Customer is null - defaulting to prevent NullPointerException");
                request.setCustomer("unknown_customer");
            }

            if (request.getExchangeRate() == 0) {
                if (divisionByZeroEnabled) {
                    log.error("Exchange rate is zero - division error");
                    new BigDecimal(request.getAmount()).divide(new BigDecimal(0));
                }
                log.warn("[UAC Fix] Exchange rate was zero - defaulting to 1.0");
                request.setExchangeRate(1.0);
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
        return recentErrors.size() * 0.01;
    }

    public void setNullCustomerFailureEnabled(boolean enabled) {
        this.nullCustomerFailureEnabled = enabled;
        log.warn("Anomaly toggle changed: nullCustomerFailureEnabled={}", enabled);
    }

    public void setDivisionByZeroEnabled(boolean enabled) {
        this.divisionByZeroEnabled = enabled;
        log.warn("Anomaly toggle changed: divisionByZeroEnabled={}", enabled);
    }

    public void injectForcedFailures(int count) {
        this.forcedFailuresRemaining = Math.max(0, count);
        log.warn("Forced payment failures scheduled: {}", this.forcedFailuresRemaining);
    }

    public void clearRecentErrors() {
        recentErrors.clear();
        log.info("Recent payment error history cleared");
    }

    public Map<String, Object> anomalyState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("null_customer_failure_enabled", nullCustomerFailureEnabled);
        state.put("division_by_zero_enabled", divisionByZeroEnabled);
        state.put("forced_failures_remaining", forcedFailuresRemaining);
        state.put("recent_errors", recentErrors.size());
        return state;
    }
}
