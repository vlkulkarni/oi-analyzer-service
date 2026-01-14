package com.oi.market.service.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.Collection;

@Component
public class MarketDataCache {
    private static final Logger log = LoggerFactory.getLogger(MarketDataCache.class);

    public static class LtpcData {
        private String instrumentKey;
        private double ltp;
        private long ltt;
        private long ltq;
        private double cp;
        private long timestamp;

        public LtpcData() {}
        public LtpcData(String instrumentKey, double ltp, long ltt, long ltq, double cp, long timestamp) {
            this.instrumentKey = instrumentKey;
            this.ltp = ltp;
            this.ltt = ltt;
            this.ltq = ltq;
            this.cp = cp;
            this.timestamp = timestamp;
        }
        public String getInstrumentKey() { return instrumentKey; }
        public void setInstrumentKey(String instrumentKey) { this.instrumentKey = instrumentKey; }
        public double getLtp() { return ltp; }
        public void setLtp(double ltp) { this.ltp = ltp; }
        public long getLtt() { return ltt; }
        public void setLtt(long ltt) { this.ltt = ltt; }
        public long getLtq() { return ltq; }
        public void setLtq(long ltq) { this.ltq = ltq; }
        public double getCp() { return cp; }
        public void setCp(double cp) { this.cp = cp; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class OhlcData {
        private String instrumentKey;
        private String interval;
        private double open;
        private double high;
        private double low;
        private double close;
        private long volume;
        private long timestamp;

        public OhlcData() {}
        public OhlcData(String instrumentKey, String interval, double open, double high, double low,
                       double close, long volume, long timestamp) {
            this.instrumentKey = instrumentKey;
            this.interval = interval;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.timestamp = timestamp;
        }
        public String getInstrumentKey() { return instrumentKey; }
        public void setInstrumentKey(String instrumentKey) { this.instrumentKey = instrumentKey; }
        public String getInterval() { return interval; }
        public void setInterval(String interval) { this.interval = interval; }
        public double getOpen() { return open; }
        public void setOpen(double open) { this.open = open; }
        public double getHigh() { return high; }
        public void setHigh(double high) { this.high = high; }
        public double getLow() { return low; }
        public void setLow(double low) { this.low = low; }
        public double getClose() { return close; }
        public void setClose(double close) { this.close = close; }
        public long getVolume() { return volume; }
        public void setVolume(long volume) { this.volume = volume; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    private final ConcurrentHashMap<String, LtpcData> ltpcCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<OhlcData>> ohlcCache = new ConcurrentHashMap<>();
    private final int maxOhlcHistorySize = 100;

    public void cacheLtpc(String instrumentKey, double ltp, long ltt, long ltq, double cp) {
        long now = System.currentTimeMillis();
        LtpcData data = new LtpcData(instrumentKey, ltp, ltt, ltq, cp, now);
        ltpcCache.put(instrumentKey, data);
    }

    public LtpcData getLtpc(String instrumentKey) {
        return ltpcCache.get(instrumentKey);
    }

    public Collection<LtpcData> getAllLtpc() {
        return ltpcCache.values();
    }

    public void cacheOhlc(String instrumentKey, String interval, double open, double high, 
                         double low, double close, long volume, long timestamp) {
        OhlcData data = new OhlcData(instrumentKey, interval, open, high, low, close, volume, timestamp);
        ConcurrentLinkedQueue<OhlcData> queue = ohlcCache.computeIfAbsent(instrumentKey, 
            k -> new ConcurrentLinkedQueue<>());
        queue.offer(data);
        while (queue.size() > maxOhlcHistorySize) {
            queue.poll();
        }
    }

    public ConcurrentLinkedQueue<OhlcData> getOhlcHistory(String instrumentKey) {
        return ohlcCache.getOrDefault(instrumentKey, new ConcurrentLinkedQueue<>());
    }

    public int getCachedInstrumentCount() {
        return ltpcCache.size();
    }

    public void clearAll() {
        ltpcCache.clear();
        ohlcCache.clear();
        log.info("Market data cache cleared");
    }

    public Map<String, Object> getStats() {
        return Map.of(
            "ltpcCacheSize", ltpcCache.size(),
            "ohlcCacheInstruments", ohlcCache.size(),
            "timestamp", System.currentTimeMillis()
        );
    }
}
