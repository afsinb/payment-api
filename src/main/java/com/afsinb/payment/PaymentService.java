package com.afsinb.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private volatile boolean nullCustomerFailureEnabled = false;
    private volatile boolean divisionByZeroEnabled = true;
    private volatile int forcedFailuresRemaining = 0;
    private volatile boolean autoChaosEnabled = true;

    private final Random random = new Random();
    private final List<String> recentErrors = new ArrayList<>();

    public Payment processPayment(PaymentRequest request) {
        try {
            maybeInjectNaturalAnomaly(request);
            log.info("Processing payment: amount={}, currency={}", request.getAmount(), request.getCurrency());

            if (forcedFailuresRemaining > 0) {
                forcedFailuresRemaining--;
                log.error("Forced payment failure triggered. Remaining={}", forcedFailuresRemaining);
                throw new IllegalStateException("Injected payment failure");
            }

            if (request.getCustomer() == null) {
                if (nullCustomerFailureEnabled) {
                    log.error("Customer object is null");
                    throw new NullPointerException("Customer cannot be null");
                }
                log.warn("Customer is null. Applying fallback customer id");
                request.setCustomer("unknown_customer");
            }

            if (request.getExchangeRate() == 0) {
                if (divisionByZeroEnabled) {
                    log.error("Exchange rate is zero. Triggering arithmetic failure");
                    new BigDecimal(request.getAmount()).divide(new BigDecimal(0));
                }
                log.warn("Exchange rate was zero. Applying safe default 1.0");
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
            recentErrors.add(String.valueOf(e.getMessage()));
            if (recentErrors.size() > 100) {
                recentErrors.remove(0);
            }
            throw e;
        }
    }

    private void maybeInjectNaturalAnomaly(PaymentRequest request) {
        if (!autoChaosEnabled) {
            return;
        }

        int roll = random.nextInt(100);
        if (roll >= 30) {
            return;
        }

        int anomaly = random.nextInt(4);
        if (anomaly == 0) {
            nullCustomerFailureEnabled = true;
            request.setCustomer(null);
            log.warn("Natural anomaly: null customer payload introduced");
            return;
        }

        if (anomaly == 1) {
            divisionByZeroEnabled = true;
            request.setExchangeRate(0.0);
            log.warn("Natural anomaly: zero exchange rate introduced");
            return;
        }

        if (anomaly == 2) {
            log.error("Unknown anomaly: PaymentLedgerMismatchException");
            throw new PaymentLedgerMismatchException("Ledger mismatch detected while reconciling payment");
        }

        forcedFailuresRemaining = Math.max(forcedFailuresRemaining, 1);
        log.warn("Natural anomaly: forced failure scheduled for this request");
    }

    public int getErrorCount() {
        return recentErrors.size();
    }

    public double getErrorRate() {
        return recentErrors.size() * 0.01;
    }

    public long getMemoryUsageMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
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

    public void setAutoChaosEnabled(boolean enabled) {
        this.autoChaosEnabled = enabled;
        log.warn("Anomaly toggle changed: autoChaosEnabled={}", enabled);
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
        state.put("auto_chaos_enabled", autoChaosEnabled);
        state.put("recent_errors", recentErrors.size());
        return state;
    }

    static class PaymentLedgerMismatchException extends RuntimeException {
        PaymentLedgerMismatchException(String message) {
            super(message);
        }
    }
}
