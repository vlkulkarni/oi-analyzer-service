package com.oi.market.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SubscriptionRequest {
    @JsonProperty("instrumentKeys")
    private List<String> instrumentKeys;
    
    @JsonProperty("mode")
    private String mode; // ltpc, option_greeks, full, full_d30
}
