package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.api.dto.QuoteStreamSubscription;
import com.myqyl.aitradex.config.MarketDataProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for streaming stock quotes at regular intervals.
 * Supports subscribing to quote updates with automatic polling every 10 seconds.
 * Subscriptions automatically expire after 5 minutes unless renewed.
 */
@Service
public class QuoteStreamingService {

  private static final Logger log = LoggerFactory.getLogger(QuoteStreamingService.class);
  private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(10);
  private static final Duration DEFAULT_MAX_DURATION = Duration.ofMinutes(5);

  private final MarketDataService marketDataService;
  private final MarketDataProperties marketDataProperties;
  private final ScheduledExecutorService scheduler;
  private final Map<String, ActiveSubscription> subscriptions = new ConcurrentHashMap<>();

  @Autowired
  public QuoteStreamingService(MarketDataService marketDataService, MarketDataProperties marketDataProperties) {
    this.marketDataService = marketDataService;
    this.marketDataProperties = marketDataProperties;
    this.scheduler = Executors.newScheduledThreadPool(4, r -> {
      Thread thread = new Thread(r, "quote-stream-");
      thread.setDaemon(true);
      return thread;
    });
  }

  // Constructor for testing
  QuoteStreamingService(
      MarketDataService marketDataService,
      MarketDataProperties marketDataProperties,
      ScheduledExecutorService scheduler) {
    this.marketDataService = marketDataService;
    this.marketDataProperties = marketDataProperties;
    this.scheduler = scheduler;
  }

  /**
   * Subscribe to quote updates for a symbol.
   * Quotes will be fetched every 10 seconds and passed to the callback.
   * The subscription automatically expires after 5 minutes.
   *
   * @param symbol the stock symbol to subscribe to
   * @param source the market data source (null for default)
   * @param callback callback to receive quote updates
   * @return subscription details including ID for cancellation
   */
  public QuoteStreamSubscription subscribe(String symbol, String source, Consumer<MarketDataQuoteDto> callback) {
    return subscribe(symbol, source, callback, DEFAULT_POLL_INTERVAL, DEFAULT_MAX_DURATION);
  }

  /**
   * Subscribe to quote updates with custom intervals.
   *
   * @param symbol the stock symbol to subscribe to
   * @param source the market data source (null for default)
   * @param callback callback to receive quote updates
   * @param pollInterval how often to fetch quotes
   * @param maxDuration maximum duration before auto-cancellation
   * @return subscription details including ID for cancellation
   */
  public QuoteStreamSubscription subscribe(
      String symbol,
      String source,
      Consumer<MarketDataQuoteDto> callback,
      Duration pollInterval,
      Duration maxDuration) {
    
    String subscriptionId = UUID.randomUUID().toString();
    String normalizedSymbol = symbol.toUpperCase();
    String resolvedSource = source != null && !source.isBlank() 
        ? source 
        : marketDataProperties.getDefaultSource();
    
    Instant startTime = Instant.now();
    Instant expiresAt = startTime.plus(maxDuration);

    log.info("Creating quote subscription {} for {} from {} (expires at {})",
        subscriptionId, normalizedSymbol, resolvedSource, expiresAt);

    // Create the polling task
    Runnable pollTask = () -> {
      try {
        ActiveSubscription sub = subscriptions.get(subscriptionId);
        if (sub == null || Instant.now().isAfter(sub.expiresAt())) {
          log.info("Subscription {} expired, cancelling", subscriptionId);
          cancel(subscriptionId);
          return;
        }

        MarketDataQuoteDto quote = marketDataService.latestQuote(normalizedSymbol, resolvedSource);
        sub.incrementPollCount();
        callback.accept(quote);
        
        log.debug("Delivered quote for {} to subscription {} (poll #{})",
            normalizedSymbol, subscriptionId, sub.getPollCount());
      } catch (Exception ex) {
        log.error("Error fetching quote for subscription {}: {}", subscriptionId, ex.getMessage());
        // Don't cancel on error - let the subscription continue
      }
    };

    // Schedule the polling task
    ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
        pollTask,
        0, // Start immediately
        pollInterval.toMillis(),
        TimeUnit.MILLISECONDS);

    // Store the subscription
    ActiveSubscription subscription = new ActiveSubscription(
        subscriptionId,
        normalizedSymbol,
        resolvedSource,
        startTime,
        expiresAt,
        pollInterval,
        future);
    subscriptions.put(subscriptionId, subscription);

    return new QuoteStreamSubscription(
        subscriptionId,
        normalizedSymbol,
        resolvedSource,
        startTime,
        expiresAt,
        pollInterval.toMillis());
  }

  /**
   * Cancel an active subscription.
   *
   * @param subscriptionId the subscription ID to cancel
   * @return true if cancelled, false if not found
   */
  public boolean cancel(String subscriptionId) {
    ActiveSubscription subscription = subscriptions.remove(subscriptionId);
    if (subscription != null) {
      subscription.future().cancel(false);
      log.info("Cancelled subscription {} for {} (delivered {} quotes)",
          subscriptionId, subscription.symbol(), subscription.getPollCount());
      return true;
    }
    return false;
  }

  /**
   * Get the status of an active subscription.
   *
   * @param subscriptionId the subscription ID
   * @return subscription details or null if not found
   */
  public QuoteStreamSubscription getSubscription(String subscriptionId) {
    ActiveSubscription sub = subscriptions.get(subscriptionId);
    if (sub == null) {
      return null;
    }
    return new QuoteStreamSubscription(
        sub.id(),
        sub.symbol(),
        sub.source(),
        sub.startTime(),
        sub.expiresAt(),
        sub.pollInterval().toMillis());
  }

  /**
   * Check if a subscription is active.
   *
   * @param subscriptionId the subscription ID
   * @return true if active, false otherwise
   */
  public boolean isActive(String subscriptionId) {
    ActiveSubscription sub = subscriptions.get(subscriptionId);
    return sub != null && Instant.now().isBefore(sub.expiresAt());
  }

  /**
   * Get the number of active subscriptions.
   */
  public int activeSubscriptionCount() {
    // Clean up expired subscriptions
    subscriptions.entrySet().removeIf(entry -> {
      if (Instant.now().isAfter(entry.getValue().expiresAt())) {
        entry.getValue().future().cancel(false);
        return true;
      }
      return false;
    });
    return subscriptions.size();
  }

  /**
   * Cancel all active subscriptions.
   */
  public void cancelAll() {
    subscriptions.forEach((id, sub) -> {
      sub.future().cancel(false);
      log.info("Cancelled subscription {} during shutdown", id);
    });
    subscriptions.clear();
  }

  /**
   * Shutdown the streaming service.
   */
  public void shutdown() {
    cancelAll();
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException ex) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private static class ActiveSubscription {
    private final String id;
    private final String symbol;
    private final String source;
    private final Instant startTime;
    private final Instant expiresAt;
    private final Duration pollInterval;
    private final ScheduledFuture<?> future;
    private int pollCount = 0;

    ActiveSubscription(
        String id,
        String symbol,
        String source,
        Instant startTime,
        Instant expiresAt,
        Duration pollInterval,
        ScheduledFuture<?> future) {
      this.id = id;
      this.symbol = symbol;
      this.source = source;
      this.startTime = startTime;
      this.expiresAt = expiresAt;
      this.pollInterval = pollInterval;
      this.future = future;
    }

    String id() { return id; }
    String symbol() { return symbol; }
    String source() { return source; }
    Instant startTime() { return startTime; }
    Instant expiresAt() { return expiresAt; }
    Duration pollInterval() { return pollInterval; }
    ScheduledFuture<?> future() { return future; }
    
    synchronized int getPollCount() { return pollCount; }
    synchronized void incrementPollCount() { pollCount++; }
  }
}
