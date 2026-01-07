package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.api.dto.QuoteStreamSubscription;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.service.QuoteStreamingService;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST API for streaming stock quotes.
 * Supports both SSE (Server-Sent Events) streaming and polling-based subscriptions.
 */
@RestController
@RequestMapping("/api/quotes/stream")
public class QuoteStreamController {

  private final QuoteStreamingService streamingService;

  public QuoteStreamController(QuoteStreamingService streamingService) {
    this.streamingService = streamingService;
  }

  /**
   * Start streaming quotes for a symbol using Server-Sent Events.
   * Quotes are pushed every 10 seconds for up to 5 minutes.
   *
   * @param symbol the stock symbol
   * @param source optional market data source (default: configured default)
   * @return SSE emitter for receiving quote updates
   */
  @GetMapping(value = "/{symbol}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamQuotes(
      @PathVariable @NotBlank String symbol,
      @RequestParam(value = "source", required = false) String source) {
    
    // 5 minutes timeout
    SseEmitter emitter = new SseEmitter(Duration.ofMinutes(5).toMillis());
    
    // Use AtomicReference to allow referencing subscription in lambda
    java.util.concurrent.atomic.AtomicReference<QuoteStreamSubscription> subscriptionRef = 
        new java.util.concurrent.atomic.AtomicReference<>();

    QuoteStreamSubscription subscription = streamingService.subscribe(
        symbol,
        source,
        quote -> {
          try {
            emitter.send(SseEmitter.event()
                .name("quote")
                .data(quote));
          } catch (Exception ex) {
            // Client disconnected, cancel subscription
            QuoteStreamSubscription sub = subscriptionRef.get();
            if (sub != null) {
              streamingService.cancel(sub.subscriptionId());
            }
          }
        });
    
    subscriptionRef.set(subscription);

    emitter.onCompletion(() -> streamingService.cancel(subscription.subscriptionId()));
    emitter.onTimeout(() -> streamingService.cancel(subscription.subscriptionId()));
    emitter.onError(ex -> streamingService.cancel(subscription.subscriptionId()));

    // Send initial subscription info
    try {
      emitter.send(SseEmitter.event()
          .name("subscribed")
          .data(subscription));
    } catch (Exception ex) {
      // Ignore
    }

    return emitter;
  }

  /**
   * Create a subscription for polling quotes.
   * Returns a subscription ID that can be used to poll for quotes.
   *
   * @param symbol the stock symbol
   * @param source optional market data source
   * @param pollIntervalMs optional poll interval in milliseconds (default: 10000)
   * @param maxDurationMs optional max duration in milliseconds (default: 300000 = 5 min)
   * @return subscription details
   */
  @PostMapping("/subscribe")
  public QuoteStreamSubscription subscribe(
      @RequestParam @NotBlank String symbol,
      @RequestParam(required = false) String source,
      @RequestParam(required = false, defaultValue = "10000") long pollIntervalMs,
      @RequestParam(required = false, defaultValue = "300000") long maxDurationMs) {
    
    ConcurrentLinkedQueue<MarketDataQuoteDto> buffer = new ConcurrentLinkedQueue<>();
    
    return streamingService.subscribe(
        symbol,
        source,
        buffer::add,
        Duration.ofMillis(pollIntervalMs),
        Duration.ofMillis(maxDurationMs));
  }

  /**
   * Get the status of a subscription.
   *
   * @param subscriptionId the subscription ID
   * @return subscription details
   */
  @GetMapping("/subscription/{subscriptionId}")
  public QuoteStreamSubscription getSubscription(@PathVariable String subscriptionId) {
    QuoteStreamSubscription subscription = streamingService.getSubscription(subscriptionId);
    if (subscription == null) {
      throw new NotFoundException("Subscription %s not found".formatted(subscriptionId));
    }
    return subscription;
  }

  /**
   * Cancel a subscription.
   *
   * @param subscriptionId the subscription ID
   * @return cancellation result
   */
  @DeleteMapping("/subscription/{subscriptionId}")
  public Map<String, Object> cancelSubscription(@PathVariable String subscriptionId) {
    boolean cancelled = streamingService.cancel(subscriptionId);
    return Map.of(
        "subscriptionId", subscriptionId,
        "cancelled", cancelled);
  }

  /**
   * Get streaming service statistics.
   *
   * @return statistics including active subscription count
   */
  @GetMapping("/stats")
  public Map<String, Object> stats() {
    return Map.of(
        "activeSubscriptions", streamingService.activeSubscriptionCount());
  }
}
