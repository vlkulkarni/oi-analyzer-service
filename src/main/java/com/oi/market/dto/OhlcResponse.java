package com.oi.market.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OhlcResponse {
    @JsonProperty("instrumentKey")
    private String instrumentKey;
    
    @JsonProperty("interval")
    private String interval;
    
    @JsonProperty("open")
    private double open;
    
    @JsonProperty("high")
    private double high;
    
    @JsonProperty("low")
    private double low;
    
    @JsonProperty("close")
    private double close;
    
    @JsonProperty("volume")
    private long volume;
    
    @JsonProperty("timestamp")
    private long timestamp;
}
