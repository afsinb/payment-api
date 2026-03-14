package com.afsinb.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String customer;
    private String idempotencyKey;
    private double amount;
    private String currency;
    private double exchangeRate;
}
