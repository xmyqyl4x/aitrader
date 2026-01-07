package com.myqyl.aitradex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.config.MarketDataProperties;
import com.myqyl.aitradex.marketdata.MarketDataAdapter;
import com.myqyl.aitradex.repository.QuoteSnapshotRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MarketDataServiceTest {

  @Test
  void latestQuoteUsesCacheWithinTtl() {
    AtomicInteger calls = new AtomicInteger();
    MarketDataQuoteDto quote =
        new MarketDataQuoteDto(
            "AAPL",
            OffsetDateTime.now(),
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.TEN,
            10L,
            "test");
    MarketDataAdapter adapter = new CountingAdapter(calls, quote);

    MarketDataProperties properties = new MarketDataProperties();
    properties.setDefaultSource("test");
    properties.setCacheTtl(Duration.ofMinutes(5));

    QuoteSnapshotRepository repository = mock(QuoteSnapshotRepository.class);
    MarketDataService service =
        new MarketDataService(List.of(adapter), repository, properties);

    service.latestQuote("AAPL", "test");
    service.latestQuote("AAPL", "test");

    assertEquals(1, calls.get());
  }

  @Test
  void purgeExpiredRemovesExpiredEntries() {
    AtomicInteger calls = new AtomicInteger();
    MarketDataQuoteDto quote =
        new MarketDataQuoteDto(
            "AAPL",
            OffsetDateTime.now(),
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.TEN,
            10L,
            "test");
    MarketDataAdapter adapter = new CountingAdapter(calls, quote);

    MarketDataProperties properties = new MarketDataProperties();
    properties.setDefaultSource("test");
    properties.setCacheTtl(Duration.ofSeconds(-1));

    QuoteSnapshotRepository repository = mock(QuoteSnapshotRepository.class);
    MarketDataService service =
        new MarketDataService(List.of(adapter), repository, properties);

    service.latestQuote("AAPL", "test");
    service.purgeExpired();
    service.latestQuote("AAPL", "test");

    assertEquals(2, calls.get());
  }

  private static final class CountingAdapter implements MarketDataAdapter {
    private final AtomicInteger calls;
    private final MarketDataQuoteDto quote;

    private CountingAdapter(AtomicInteger calls, MarketDataQuoteDto quote) {
      this.calls = calls;
      this.quote = quote;
    }

    @Override
    public String name() {
      return "test";
    }

    @Override
    public MarketDataQuoteDto latestQuote(String symbol) {
      calls.incrementAndGet();
      return quote;
    }
  }
}
