package com.oi.market.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MarketDataResponse {
    @JsonProperty("instrumentKey")
    private String instrumentKey;
    
    @JsonProperty("ltp")
    private double ltp; // last traded price
    
    @JsonProperty("ltt")
    private long ltt; // last traded time
    
    @JsonProperty("ltq")
    private long ltq; // last traded quantity
    
    @JsonProperty("cp")
    private double cp; // close price
    
    @JsonProperty("timestamp")
    private long timestamp;
}
