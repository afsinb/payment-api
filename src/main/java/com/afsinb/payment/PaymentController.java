package com.afsinb.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public Payment processPayment(@RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "payment-api",
            "errors", paymentService.getErrorCount(),
            "error_rate", paymentService.getErrorRate()
        );
    }

    @GetMapping("/admin/anomalies")
    public Map<String, Object> anomalyState() {
        return paymentService.anomalyState();
    }

    @PostMapping("/admin/anomalies")
    public Map<String, Object> configureAnomalies(
            @RequestParam(required = false) Boolean nullCustomerFailureEnabled,
            @RequestParam(required = false) Boolean divisionByZeroEnabled,
            @RequestParam(required = false) Integer forcedFailures,
            @RequestParam(required = false, defaultValue = "false") boolean clearErrors
    ) {
        if (nullCustomerFailureEnabled != null) {
            paymentService.setNullCustomerFailureEnabled(nullCustomerFailureEnabled);
        }
        if (divisionByZeroEnabled != null) {
            paymentService.setDivisionByZeroEnabled(divisionByZeroEnabled);
        }
        if (forcedFailures != null) {
            paymentService.injectForcedFailures(forcedFailures);
        }
        if (clearErrors) {
            paymentService.clearRecentErrors();
        }
        return paymentService.anomalyState();
    }
}
