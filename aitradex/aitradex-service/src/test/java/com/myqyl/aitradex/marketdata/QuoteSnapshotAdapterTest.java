package com.myqyl.aitradex.marketdata;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.domain.QuoteSnapshot;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.QuoteSnapshotRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuoteSnapshotAdapterTest {

  @Mock
  private QuoteSnapshotRepository repository;

  private QuoteSnapshotAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new QuoteSnapshotAdapter(repository);
  }

  @Test
  void name_returnsQuoteSnapshots() {
    assertEquals("quote-snapshots", adapter.name());
  }

  @Test
  void latestQuote_returnsLatestSnapshot() {
    OffsetDateTime now = OffsetDateTime.now();
    QuoteSnapshot snapshot = QuoteSnapshot.builder()
        .id(UUID.randomUUID())
        .symbol("AAPL")
        .asOf(now)
        .open(new BigDecimal("150.00"))
        .high(new BigDecimal("152.00"))
        .low(new BigDecimal("149.00"))
        .close(new BigDecimal("151.50"))
        .volume(50000000L)
        .source("manual")
        .build();

    when(repository.findBySymbolOrderByAsOfDesc("AAPL")).thenReturn(List.of(snapshot));

    MarketDataQuoteDto quote = adapter.latestQuote("AAPL");

    assertNotNull(quote);
    assertEquals("AAPL", quote.symbol());
    assertEquals(now, quote.asOf());
    assertEquals(new BigDecimal("150.00"), quote.open());
    assertEquals(new BigDecimal("152.00"), quote.high());
    assertEquals(new BigDecimal("149.00"), quote.low());
    assertEquals(new BigDecimal("151.50"), quote.close());
    assertEquals(50000000L, quote.volume());
    assertEquals("manual", quote.source());
  }

  @Test
  void latestQuote_returnsFirstSnapshotWhenMultipleExist() {
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime earlier = now.minusHours(1);
    
    QuoteSnapshot latestSnapshot = QuoteSnapshot.builder()
        .id(UUID.randomUUID())
        .symbol("AAPL")
        .asOf(now)
        .open(new BigDecimal("151.00"))
        .high(new BigDecimal("153.00"))
        .low(new BigDecimal("150.00"))
        .close(new BigDecimal("152.00"))
        .volume(60000000L)
        .source("api")
        .build();

    QuoteSnapshot olderSnapshot = QuoteSnapshot.builder()
        .id(UUID.randomUUID())
        .symbol("AAPL")
        .asOf(earlier)
        .open(new BigDecimal("150.00"))
        .high(new BigDecimal("152.00"))
        .low(new BigDecimal("149.00"))
        .close(new BigDecimal("151.00"))
        .volume(50000000L)
        .source("api")
        .build();

    when(repository.findBySymbolOrderByAsOfDesc("AAPL"))
        .thenReturn(List.of(latestSnapshot, olderSnapshot));

    MarketDataQuoteDto quote = adapter.latestQuote("AAPL");

    assertEquals(now, quote.asOf());
    assertEquals(new BigDecimal("152.00"), quote.close());
  }

  @Test
  void latestQuote_throwsNotFoundWhenNoSnapshots() {
    when(repository.findBySymbolOrderByAsOfDesc("UNKNOWN"))
        .thenReturn(Collections.emptyList());

    NotFoundException ex = assertThrows(NotFoundException.class, 
        () -> adapter.latestQuote("UNKNOWN"));
    
    assertTrue(ex.getMessage().contains("UNKNOWN"));
  }

  @Test
  void latestQuote_normalizesSymbolToUppercase() {
    OffsetDateTime now = OffsetDateTime.now();
    QuoteSnapshot snapshot = QuoteSnapshot.builder()
        .id(UUID.randomUUID())
        .symbol("MSFT")
        .asOf(now)
        .open(new BigDecimal("350.00"))
        .high(new BigDecimal("355.00"))
        .low(new BigDecimal("348.00"))
        .close(new BigDecimal("352.00"))
        .volume(25000000L)
        .source("manual")
        .build();

    when(repository.findBySymbolOrderByAsOfDesc("MSFT")).thenReturn(List.of(snapshot));

    MarketDataQuoteDto quote = adapter.latestQuote("msft");

    assertEquals("MSFT", quote.symbol());
    verify(repository).findBySymbolOrderByAsOfDesc("MSFT");
  }

  @Test
  void latestQuote_handlesNullVolume() {
    OffsetDateTime now = OffsetDateTime.now();
    QuoteSnapshot snapshot = QuoteSnapshot.builder()
        .id(UUID.randomUUID())
        .symbol("TEST")
        .asOf(now)
        .open(new BigDecimal("100.00"))
        .high(new BigDecimal("105.00"))
        .low(new BigDecimal("98.00"))
        .close(new BigDecimal("102.00"))
        .volume(null)
        .source("manual")
        .build();

    when(repository.findBySymbolOrderByAsOfDesc("TEST")).thenReturn(List.of(snapshot));

    MarketDataQuoteDto quote = adapter.latestQuote("TEST");

    assertNull(quote.volume());
  }

  @Test
  void latestQuote_handlesNullPriceFields() {
    OffsetDateTime now = OffsetDateTime.now();
    QuoteSnapshot snapshot = QuoteSnapshot.builder()
        .id(UUID.randomUUID())
        .symbol("PARTIAL")
        .asOf(now)
        .open(null)
        .high(null)
        .low(null)
        .close(new BigDecimal("100.00"))
        .volume(1000L)
        .source("partial")
        .build();

    when(repository.findBySymbolOrderByAsOfDesc("PARTIAL")).thenReturn(List.of(snapshot));

    MarketDataQuoteDto quote = adapter.latestQuote("PARTIAL");

    assertNull(quote.open());
    assertNull(quote.high());
    assertNull(quote.low());
    assertEquals(new BigDecimal("100.00"), quote.close());
  }
}
