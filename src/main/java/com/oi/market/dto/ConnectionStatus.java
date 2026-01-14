package com.oi.market.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConnectionStatus {
    @JsonProperty("connected")
    private boolean connected;
    
    @JsonProperty("subscriptionCount")
    private int subscriptionCount;
    
    @JsonProperty("lastUpdateTs")
    private long lastUpdateTs;
    
    @JsonProperty("lastError")
    private String lastError;
    
    @JsonProperty("reconnectCount")
    private long reconnectCount;
    
    @JsonProperty("uptime")
    private long uptime;
}
