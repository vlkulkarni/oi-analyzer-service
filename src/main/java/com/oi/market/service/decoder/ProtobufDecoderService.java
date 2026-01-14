package com.oi.market.service.decoder;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.oi.market.exception.ProtobufDecodingException;
import com.oi.market.util.ProtoMessagePool;
import com.upstox.marketdatafeederv3udapi.rpc.proto.MarketDataFeedProto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Protobuf message decoder service.
 * Decodes binary WebSocket frames into FeedResponse objects.
 * Uses object pooling for efficient buffer management.
 */
@Service
public class ProtobufDecoderService {

    private static final Logger logger = LoggerFactory.getLogger(ProtobufDecoderService.class);

    private final ProtoMessagePool messagePool;

    public ProtobufDecoderService(ProtoMessagePool messagePool) {
        this.messagePool = messagePool;
    }

    /**
     * Decode binary message into FeedResponse
     */
    public FeedResponse decodeFeedResponse(byte[] messageBytes) throws ProtobufDecodingException {
        if (messageBytes == null || messageBytes.length == 0) {
            throw new ProtobufDecodingException("Message bytes are empty");
        }

        try {
            // Use CodedInputStream for efficient parsing
            ByteArrayInputStream bais = new ByteArrayInputStream(messageBytes);
            CodedInputStream cis = CodedInputStream.newInstance(bais);

            FeedResponse response = FeedResponse.parseFrom(cis);

            if (response == null) {
                throw new ProtobufDecodingException("Parsed FeedResponse is null");
            }

            logger.debug("Decoded FeedResponse: type={}, feedCount={}, currentTs={}",
                    response.getType(),
                    response.getFeedsCount(),
                    response.getCurrentTs());

            return response;
        } catch (InvalidProtocolBufferException e) {
            throw new ProtobufDecodingException("Invalid protobuf message format", e);
        } catch (IOException e) {
            throw new ProtobufDecodingException("Failed to decode protobuf message", e);
        } catch (Exception e) {
            throw new ProtobufDecodingException("Unexpected error during decoding", e);
        }
    }

    /**
     * Extract LTPC data from FeedResponse
     */
    public LTPC extractLtpc(FeedResponse response, String instrumentKey) throws ProtobufDecodingException {
        if (response == null || !response.getFeedsMap().containsKey(instrumentKey)) {
            return null;
        }

        try {
            Feed feed = response.getFeedsMap().get(instrumentKey);

            if (feed != null && feed.hasLtpc()) {
                LTPC ltpc = feed.getLtpc();
                logger.debug("Extracted LTPC for {}: ltp={}, ltt={}, ltq={}, cp={}",
                        instrumentKey,
                        ltpc.getLtp(),
                        ltpc.getLtt(),
                        ltpc.getLtq(),
                        ltpc.getCp());
                return ltpc;
            }

            return null;
        } catch (Exception e) {
            throw new ProtobufDecodingException("Failed to extract LTPC data for " + instrumentKey, e);
        }
    }

    /**
     * Extract OHLC data from FeedResponse
     */
    public MarketOHLC extractMarketOHLC(FeedResponse response, String instrumentKey) throws ProtobufDecodingException {
        if (response == null || !response.getFeedsMap().containsKey(instrumentKey)) {
            return null;
        }

        try {
            Feed feed = response.getFeedsMap().get(instrumentKey);

            if (feed != null && feed.hasFullFeed()) {
                FullFeed fullFeed = feed.getFullFeed();

                if (fullFeed != null) {
                    if (fullFeed.hasMarketFF()) {
                        MarketFullFeed marketFF = fullFeed.getMarketFF();
                        if (marketFF != null && marketFF.hasMarketOHLC()) {
                            MarketOHLC marketOHLC = marketFF.getMarketOHLC();
                            logger.debug("Extracted OHLC for {}: count={}",
                                    instrumentKey,
                                    marketOHLC.getOhlcCount());
                            return marketOHLC;
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            throw new ProtobufDecodingException("Failed to extract OHLC data for " + instrumentKey, e);
        }
    }

    /**
     * Extract market depth (bid/ask) from FeedResponse
     */
    public MarketLevel extractMarketLevel(FeedResponse response, String instrumentKey) throws ProtobufDecodingException {
        if (response == null || !response.getFeedsMap().containsKey(instrumentKey)) {
            return null;
        }

        try {
            Feed feed = response.getFeedsMap().get(instrumentKey);

            if (feed != null && feed.hasFullFeed()) {
                FullFeed fullFeed = feed.getFullFeed();

                if (fullFeed != null && fullFeed.hasMarketFF()) {
                    MarketFullFeed marketFF = fullFeed.getMarketFF();
                    if (marketFF != null && marketFF.hasMarketLevel()) {
                        MarketLevel level = marketFF.getMarketLevel();
                        logger.debug("Extracted MarketLevel for {}: quoteCount={}",
                                instrumentKey,
                                level.getBidAskQuoteCount());
                        return level;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            throw new ProtobufDecodingException("Failed to extract market level for " + instrumentKey, e);
        }
    }

    /**
     * Extract option greeks from FeedResponse
     */
    public OptionGreeks extractOptionGreeks(FeedResponse response, String instrumentKey) throws ProtobufDecodingException {
        if (response == null || !response.getFeedsMap().containsKey(instrumentKey)) {
            return null;
        }

        try {
            Feed feed = response.getFeedsMap().get(instrumentKey);

            if (feed != null) {
                if (feed.hasFullFeed()) {
                    FullFeed fullFeed = feed.getFullFeed();
                    if (fullFeed != null && fullFeed.hasMarketFF()) {
                        MarketFullFeed marketFF = fullFeed.getMarketFF();
                        if (marketFF != null && marketFF.hasOptionGreeks()) {
                            OptionGreeks greeks = marketFF.getOptionGreeks();
                            logger.debug("Extracted OptionGreeks for {}: delta={}, gamma={}, vega={}, theta={}, rho={}",
                                    instrumentKey,
                                    greeks.getDelta(),
                                    greeks.getGamma(),
                                    greeks.getVega(),
                                    greeks.getTheta(),
                                    greeks.getRho());
                            return greeks;
                        }
                    }
                } else if (feed.hasFirstLevelWithGreeks()) {
                    FirstLevelWithGreeks firstLevel = feed.getFirstLevelWithGreeks();
                    if (firstLevel != null && firstLevel.hasOptionGreeks()) {
                        return firstLevel.getOptionGreeks();
                    }
                }
            }

            return null;
        } catch (Exception e) {
            throw new ProtobufDecodingException("Failed to extract option greeks for " + instrumentKey, e);
        }
    }

    /**
     * Get message type name for logging
     */
    public String getMessageTypeName(FeedResponse response) {
        if (response == null) {
            return "null";
        }

        try {
            return response.getType().toString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
