package com.bharath.fraud.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("amount")
    private double amount;

    @JsonProperty("merchant")
    private String merchant;

    @JsonProperty("merchant_category")
    private String merchantCategory;

    @JsonProperty("country")
    private String country;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("card_present")
    private boolean cardPresent;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("hour_of_day")
    private int hourOfDay;

    @JsonProperty("day_of_week")
    private int dayOfWeek;
}
