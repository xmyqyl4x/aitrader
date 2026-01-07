package com.myqyl.aitradex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.api.dto.QuoteStreamSubscription;
import com.myqyl.aitradex.config.MarketDataProperties;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class QuoteStreamingServiceTest {

  @Mock
  private MarketDataService marketDataService;

  @Mock
  private ScheduledExecutorService scheduler;

  @Mock
  private ScheduledFuture<Object> scheduledFuture;

  private MarketDataProperties properties;
  private QuoteStreamingService streamingService;

  @BeforeEach
  void setUp() {
    properties = new MarketDataProperties();
    properties.setDefaultSource("alphavantage");
    streamingService = new QuoteStreamingService(marketDataService, properties, scheduler);
  }

  private void mockScheduler() {
    doReturn(scheduledFuture).when(scheduler)
        .scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
  }

  @Test
  void subscribe_createsSubscriptionWithDefaults() {
    mockScheduler();

    AtomicReference<MarketDataQuoteDto> receivedQuote = new AtomicReference<>();
    QuoteStreamSubscription subscription = streamingService.subscribe("AAPL", null, receivedQuote::set);

    assertNotNull(subscription);
    assertNotNull(subscription.subscriptionId());
    assertEquals("AAPL", subscription.symbol());
    assertEquals("alphavantage", subscription.source());
    assertEquals(10000L, subscription.pollIntervalMs());
    assertNotNull(subscription.startedAt());
    assertNotNull(subscription.expiresAt());
    
    // Verify expiration is approximately 5 minutes from now
    long expirationMs = Duration.between(subscription.startedAt(), subscription.expiresAt()).toMillis();
    assertEquals(300000L, expirationMs, 1000L); // Within 1 second tolerance
  }

  @Test
  void subscribe_usesProvidedSource() {
    mockScheduler();

    QuoteStreamSubscription subscription = streamingService.subscribe("AAPL", "yahoo", quote -> {});

    assertEquals("yahoo", subscription.source());
  }

  @Test
  void subscribe_usesCustomIntervals() {
    mockScheduler();

    QuoteStreamSubscription subscription = streamingService.subscribe(
        "AAPL",
        null,
        quote -> {},
        Duration.ofSeconds(5),
        Duration.ofMinutes(2));

    assertEquals(5000L, subscription.pollIntervalMs());
    
    long expirationMs = Duration.between(subscription.startedAt(), subscription.expiresAt()).toMillis();
    assertEquals(120000L, expirationMs, 1000L);
  }

  @Test
  void subscribe_schedulesPollingTask() {
    mockScheduler();

    streamingService.subscribe("AAPL", null, quote -> {});

    verify(scheduler).scheduleAtFixedRate(
        any(Runnable.class),
        eq(0L),
        eq(10000L),
        eq(TimeUnit.MILLISECONDS));
  }

  @Test
  void subscribe_pollTaskFetchesQuoteAndCallsCallback() {
    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
    doReturn(scheduledFuture).when(scheduler)
        .scheduleAtFixedRate(taskCaptor.capture(), anyLong(), anyLong(), any(TimeUnit.class));

    MarketDataQuoteDto expectedQuote = new MarketDataQuoteDto(
        "AAPL",
        OffsetDateTime.now(),
        new BigDecimal("150.00"),
        new BigDecimal("152.00"),
        new BigDecimal("149.00"),
        new BigDecimal("151.50"),
        50000000L,
        "alphavantage");

    when(marketDataService.latestQuote("AAPL", "alphavantage")).thenReturn(expectedQuote);

    AtomicReference<MarketDataQuoteDto> receivedQuote = new AtomicReference<>();
    streamingService.subscribe("AAPL", null, receivedQuote::set);

    // Execute the scheduled task
    taskCaptor.getValue().run();

    assertEquals(expectedQuote, receivedQuote.get());
    verify(marketDataService).latestQuote("AAPL", "alphavantage");
  }

  @Test
  void cancel_cancelsScheduledFutureAndReturnsTrue() {
    mockScheduler();

    QuoteStreamSubscription subscription = streamingService.subscribe("AAPL", null, quote -> {});

    boolean result = streamingService.cancel(subscription.subscriptionId());

    assertTrue(result);
    verify(scheduledFuture).cancel(false);
  }

  @Test
  void cancel_returnsFalseForNonExistentSubscription() {
    boolean result = streamingService.cancel("non-existent-id");

    assertFalse(result);
  }

  @Test
  void getSubscription_returnsSubscriptionDetails() {
    mockScheduler();

    QuoteStreamSubscription original = streamingService.subscribe("AAPL", null, quote -> {});
    QuoteStreamSubscription retrieved = streamingService.getSubscription(original.subscriptionId());

    assertNotNull(retrieved);
    assertEquals(original.subscriptionId(), retrieved.subscriptionId());
    assertEquals(original.symbol(), retrieved.symbol());
    assertEquals(original.source(), retrieved.source());
  }

  @Test
  void getSubscription_returnsNullForNonExistentSubscription() {
    QuoteStreamSubscription result = streamingService.getSubscription("non-existent-id");

    assertNull(result);
  }

  @Test
  void isActive_returnsTrueForActiveSubscription() {
    mockScheduler();

    QuoteStreamSubscription subscription = streamingService.subscribe("AAPL", null, quote -> {});

    assertTrue(streamingService.isActive(subscription.subscriptionId()));
  }

  @Test
  void isActive_returnsFalseForNonExistentSubscription() {
    assertFalse(streamingService.isActive("non-existent-id"));
  }

  @Test
  void activeSubscriptionCount_returnsCorrectCount() {
    mockScheduler();

    streamingService.subscribe("AAPL", null, quote -> {});
    streamingService.subscribe("MSFT", null, quote -> {});
    streamingService.subscribe("GOOG", null, quote -> {});

    assertEquals(3, streamingService.activeSubscriptionCount());
  }

  @Test
  void cancelAll_cancelsAllSubscriptions() {
    mockScheduler();

    streamingService.subscribe("AAPL", null, quote -> {});
    streamingService.subscribe("MSFT", null, quote -> {});

    streamingService.cancelAll();

    assertEquals(0, streamingService.activeSubscriptionCount());
    verify(scheduledFuture, times(2)).cancel(false);
  }

  @Test
  void subscribe_normalizesSymbolToUppercase() {
    mockScheduler();

    QuoteStreamSubscription subscription = streamingService.subscribe("aapl", null, quote -> {});

    assertEquals("AAPL", subscription.symbol());
  }

  @Test
  void subscribe_pollTaskHandlesErrorsGracefully() {
    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
    doReturn(scheduledFuture).when(scheduler)
        .scheduleAtFixedRate(taskCaptor.capture(), anyLong(), anyLong(), any(TimeUnit.class));

    when(marketDataService.latestQuote(any(), any()))
        .thenThrow(new RuntimeException("API error"));

    AtomicInteger callbackCount = new AtomicInteger(0);
    streamingService.subscribe("AAPL", null, quote -> callbackCount.incrementAndGet());

    // Execute the scheduled task - should not throw
    assertDoesNotThrow(() -> taskCaptor.getValue().run());
    
    // Callback should not have been called due to error
    assertEquals(0, callbackCount.get());
  }

  @Test
  void subscribe_multipleSubscriptionsForSameSymbol() {
    mockScheduler();

    QuoteStreamSubscription sub1 = streamingService.subscribe("AAPL", null, quote -> {});
    QuoteStreamSubscription sub2 = streamingService.subscribe("AAPL", null, quote -> {});

    assertNotEquals(sub1.subscriptionId(), sub2.subscriptionId());
    assertEquals(2, streamingService.activeSubscriptionCount());
  }
}
