package com.oi.market.service.processor;

import com.oi.market.service.data.MarketDataCache;
import com.oi.market.service.decoder.ProtobufDecoderService;
import com.oi.market.util.MetricsHelper;
import com.upstox.marketdatafeederv3udapi.rpc.proto.MarketDataFeedProto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Market data processor service.
 * Processes decoded FeedResponse messages, extracts LTPC/OHLC data,
 * caches them, and logs OHLC data to dedicated logger.
 */
@Service
public class MarketDataProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataProcessor.class);
    private static final Logger ohlcLogger = LoggerFactory.getLogger("com.oi.market.ohlc");

    @Value("${market-data.mode:ltpc}")
    private String dataMode;

    @Value("${market-data.limit.keys:5000}")
    private int maxKeys;

    private final ProtobufDecoderService decoderService;
    private final MarketDataCache marketDataCache;
    private final MetricsHelper metricsHelper;

    public MarketDataProcessor(ProtobufDecoderService decoderService,
                              MarketDataCache marketDataCache,
                              MetricsHelper metricsHelper) {
        this.decoderService = decoderService;
        this.marketDataCache = marketDataCache;
        this.metricsHelper = metricsHelper;
    }

    /**
     * Process FeedResponse message
     */
    public void processFeedResponse(FeedResponse response) {
        if (response == null) {
            logger.warn("Received null FeedResponse");
            return;
        }

        try {
            String messageType = decoderService.getMessageTypeName(response);
            int feedCount = response.getFeedsCount();

            logger.debug("Processing FeedResponse: type={}, feedCount={}, timestamp={}",
                    messageType,
                    feedCount,
                    response.getCurrentTs());

            // Process each feed in the response
            response.getFeedsMap().forEach((instrumentKey, feed) -> {
                processFeed(instrumentKey, feed, response.getCurrentTs());
            });

            // Process market info if present
            if (response.hasMarketInfo()) {
                processMarketInfo(response.getMarketInfo());
            }

            metricsHelper.recordMessage();

        } catch (Exception e) {
            logger.error("Error processing FeedResponse", e);
            metricsHelper.recordDecodeError();
        }
    }

    /**
     * Process individual Feed for an instrument
     */
    private void processFeed(String instrumentKey, Feed feed, long currentTs) {
        if (feed == null) {
            return;
        }

        try {
            // Check cache size limit
            if (marketDataCache.getCachedInstrumentCount() >= maxKeys) {
                logger.warn("LTPC cache at limit ({}), skipping new instrument {}",
                        maxKeys, instrumentKey);
                return;
            }

            // Process LTPC if present
            if (feed.hasLtpc()) {
                LTPC ltpc = feed.getLtpc();
                cacheAndLogLtpc(instrumentKey, ltpc, currentTs);
            }

            // Process full feed if present (includes OHLC, Greeks, etc)
            if (feed.hasFullFeed()) {
                processFullFeed(instrumentKey, feed.getFullFeed(), currentTs);
            }

            // Process first level with greeks if present
            if (feed.hasFirstLevelWithGreeks()) {
                processFirstLevelWithGreeks(instrumentKey, feed.getFirstLevelWithGreeks(), currentTs);
            }

        } catch (Exception e) {
            logger.error("Error processing feed for instrument {}", instrumentKey, e);
            metricsHelper.recordDecodeError();
        }
    }

    /**
     * Cache and log LTPC data
     */
    private void cacheAndLogLtpc(String instrumentKey, LTPC ltpc, long currentTs) {
        try {
            marketDataCache.cacheLtpc(instrumentKey, ltpc.getLtp(), ltpc.getLtt(), ltpc.getLtq(), ltpc.getCp());

            logger.debug("LTPC cached for {}: ltp={}, ltt={}, ltq={}, cp={}",
                    instrumentKey,
                    ltpc.getLtp(),
                    ltpc.getLtt(),
                    ltpc.getLtq(),
                    ltpc.getCp());

        } catch (Exception e) {
            logger.error("Error caching LTPC for {}", instrumentKey, e);
        }
    }

    /**
     * Process full feed (market data with OHLC, Greeks, etc)
     */
    private void processFullFeed(String instrumentKey, FullFeed fullFeed, long currentTs) {
        if (fullFeed == null) {
            return;
        }

        try {
            if (fullFeed.hasMarketFF()) {
                MarketFullFeed marketFF = fullFeed.getMarketFF();

                // Process LTPC from MarketFullFeed
                if (marketFF.hasLtpc()) {
                    cacheAndLogLtpc(instrumentKey, marketFF.getLtpc(), currentTs);
                }

                // Process OHLC data
                if (marketFF.hasMarketOHLC()) {
                    processMarketOHLC(instrumentKey, marketFF.getMarketOHLC());
                }

                // Process market level (depth)
                if (marketFF.hasMarketLevel()) {
                    logger.debug("Market depth for {}: {} quotes", 
                            instrumentKey,
                            marketFF.getMarketLevel().getBidAskQuoteCount());
                }

                // Process option greeks
                if (marketFF.hasOptionGreeks()) {
                    OptionGreeks greeks = marketFF.getOptionGreeks();
                    logger.debug("Greeks for {}: delta={}, gamma={}, vega={}",
                            instrumentKey,
                            greeks.getDelta(),
                            greeks.getGamma(),
                            greeks.getVega());
                }
            } else if (fullFeed.hasIndexFF()) {
                IndexFullFeed indexFF = fullFeed.getIndexFF();

                // Process LTPC from IndexFullFeed
                if (indexFF.hasLtpc()) {
                    cacheAndLogLtpc(instrumentKey, indexFF.getLtpc(), currentTs);
                }

                // Process OHLC data
                if (indexFF.hasMarketOHLC()) {
                    processMarketOHLC(instrumentKey, indexFF.getMarketOHLC());
                }
            }
        } catch (Exception e) {
            logger.error("Error processing full feed for {}", instrumentKey, e);
        }
    }

    /**
     * Process OHLC candle data
     */
    private void processMarketOHLC(String instrumentKey, MarketOHLC marketOHLC) {
        if (marketOHLC.getOhlcCount() == 0) {
            return;
        }

        try {
            for (OHLC ohlc : marketOHLC.getOhlcList()) {
                marketDataCache.cacheOhlc(
                        instrumentKey,
                        ohlc.getInterval(),
                        ohlc.getOpen(),
                        ohlc.getHigh(),
                        ohlc.getLow(),
                        ohlc.getClose(),
                        ohlc.getVol(),
                        ohlc.getTs()
                );

                // Log OHLC to dedicated logger
                ohlcLogger.info("OHLC: instrument={}, interval={}, o={}, h={}, l={}, c={}, v={}, ts={}",
                        instrumentKey,
                        ohlc.getInterval(),
                        ohlc.getOpen(),
                        ohlc.getHigh(),
                        ohlc.getLow(),
                        ohlc.getClose(),
                        ohlc.getVol(),
                        ohlc.getTs());
            }
        } catch (Exception e) {
            logger.error("Error processing OHLC for {}", instrumentKey, e);
        }
    }

    /**
     * Process first level with greeks (options)
     */
    private void processFirstLevelWithGreeks(String instrumentKey, FirstLevelWithGreeks firstLevel, long currentTs) {
        try {
            // Cache LTPC from first level
            if (firstLevel.hasLtpc()) {
                cacheAndLogLtpc(instrumentKey, firstLevel.getLtpc(), currentTs);
            }

            // Process greeks for options
            if (firstLevel.hasOptionGreeks()) {
                OptionGreeks greeks = firstLevel.getOptionGreeks();
                logger.debug("Option Greeks for {}: delta={}, theta={}, gamma={}, vega={}, rho={}",
                        instrumentKey,
                        greeks.getDelta(),
                        greeks.getTheta(),
                        greeks.getGamma(),
                        greeks.getVega(),
                        greeks.getRho());
            }
        } catch (Exception e) {
            logger.error("Error processing first level with greeks for {}", instrumentKey, e);
        }
    }

    /**
     * Process market info (status updates)
     */
    private void processMarketInfo(MarketInfo marketInfo) {
        try {
            marketInfo.getSegmentStatus().forEach((segment, status) -> {
                logger.debug("Market status: segment={}, status={}", segment, status);
            });
        } catch (Exception e) {
            logger.error("Error processing market info", e);
        }
    }

    /**
     * Get statistics about processed data
     */
    public String getProcessorStats() {
        return String.format(
                "Processor[ltpcCache=%d, mode=%s, maxKeys=%d]",
                marketDataCache.getCachedInstrumentCount(),
                dataMode,
                maxKeys
        );
    }
}
