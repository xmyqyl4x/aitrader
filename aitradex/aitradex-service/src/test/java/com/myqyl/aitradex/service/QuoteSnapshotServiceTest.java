package com.myqyl.aitradex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.myqyl.aitradex.api.dto.CreateQuoteSnapshotRequest;
import com.myqyl.aitradex.api.dto.QuoteSnapshotDto;
import com.myqyl.aitradex.domain.QuoteSnapshot;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.QuoteSnapshotRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuoteSnapshotServiceTest {

  @Mock
  private QuoteSnapshotRepository repository;

  private QuoteSnapshotService service;

  @BeforeEach
  void setUp() {
    service = new QuoteSnapshotService(repository);
  }

  @Test
  void create_savesSnapshotAndReturnsDto() {
    OffsetDateTime now = OffsetDateTime.now();
    CreateQuoteSnapshotRequest request = new CreateQuoteSnapshotRequest(
        "aapl",
        now,
        new BigDecimal("150.00"),
        new BigDecimal("152.00"),
        new BigDecimal("149.00"),
        new BigDecimal("151.50"),
        50000000L,
        "manual");

    UUID id = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.now();
    
    when(repository.save(any(QuoteSnapshot.class))).thenAnswer(invocation -> {
      QuoteSnapshot snapshot = invocation.getArgument(0);
      return QuoteSnapshot.builder()
          .id(id)
          .symbol(snapshot.getSymbol())
          .asOf(snapshot.getAsOf())
          .open(snapshot.getOpen())
          .high(snapshot.getHigh())
          .low(snapshot.getLow())
          .close(snapshot.getClose())
          .volume(snapshot.getVolume())
          .source(snapshot.getSource())
          .createdAt(createdAt)
          .build();
    });

    QuoteSnapshotDto result = service.create(request);

    assertNotNull(result);
    assertEquals(id, result.id());
    assertEquals("AAPL", result.symbol()); // Should be uppercase
    assertEquals(now, result.asOf());
    assertEquals(new BigDecimal("150.00"), result.open());
    assertEquals(new BigDecimal("152.00"), result.high());
    assertEquals(new BigDecimal("149.00"), result.low());
    assertEquals(new BigDecimal("151.50"), result.close());
    assertEquals(50000000L, result.volume());
    assertEquals("manual", result.source());

    ArgumentCaptor<QuoteSnapshot> captor = ArgumentCaptor.forClass(QuoteSnapshot.class);
    verify(repository).save(captor.capture());
    assertEquals("AAPL", captor.getValue().getSymbol());
  }

  @Test
  void list_returnsAllSnapshotsWhenSymbolIsNull() {
    OffsetDateTime now = OffsetDateTime.now();
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    QuoteSnapshot snapshot1 = createSnapshot(id1, "AAPL", now, "150.00");
    QuoteSnapshot snapshot2 = createSnapshot(id2, "MSFT", now.minusHours(1), "350.00");

    when(repository.findTop100ByOrderByAsOfDesc()).thenReturn(List.of(snapshot1, snapshot2));

    List<QuoteSnapshotDto> result = service.list(null);

    assertEquals(2, result.size());
    assertEquals("AAPL", result.get(0).symbol());
    assertEquals("MSFT", result.get(1).symbol());
    verify(repository).findTop100ByOrderByAsOfDesc();
    verify(repository, never()).findBySymbolOrderByAsOfDesc(any());
  }

  @Test
  void list_returnsSnapshotsForSymbol() {
    OffsetDateTime now = OffsetDateTime.now();
    UUID id = UUID.randomUUID();

    QuoteSnapshot snapshot = createSnapshot(id, "AAPL", now, "150.00");

    when(repository.findBySymbolOrderByAsOfDesc("AAPL")).thenReturn(List.of(snapshot));

    List<QuoteSnapshotDto> result = service.list("aapl");

    assertEquals(1, result.size());
    assertEquals("AAPL", result.get(0).symbol());
    verify(repository).findBySymbolOrderByAsOfDesc("AAPL");
  }

  @Test
  void list_returnsEmptyListWhenNoSnapshots() {
    when(repository.findBySymbolOrderByAsOfDesc("UNKNOWN")).thenReturn(Collections.emptyList());

    List<QuoteSnapshotDto> result = service.list("UNKNOWN");

    assertTrue(result.isEmpty());
  }

  @Test
  void get_returnsSnapshotById() {
    UUID id = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();
    QuoteSnapshot snapshot = createSnapshot(id, "AAPL", now, "150.00");

    when(repository.findById(id)).thenReturn(Optional.of(snapshot));

    QuoteSnapshotDto result = service.get(id);

    assertNotNull(result);
    assertEquals(id, result.id());
    assertEquals("AAPL", result.symbol());
  }

  @Test
  void get_throwsNotFoundWhenSnapshotDoesNotExist() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class, () -> service.get(id));
    assertTrue(ex.getMessage().contains(id.toString()));
  }

  @Test
  void create_handlesNullVolume() {
    OffsetDateTime now = OffsetDateTime.now();
    CreateQuoteSnapshotRequest request = new CreateQuoteSnapshotRequest(
        "TEST",
        now,
        new BigDecimal("100.00"),
        new BigDecimal("105.00"),
        new BigDecimal("98.00"),
        new BigDecimal("102.00"),
        null,
        "test");

    UUID id = UUID.randomUUID();
    when(repository.save(any(QuoteSnapshot.class))).thenAnswer(invocation -> {
      QuoteSnapshot snapshot = invocation.getArgument(0);
      return QuoteSnapshot.builder()
          .id(id)
          .symbol(snapshot.getSymbol())
          .asOf(snapshot.getAsOf())
          .open(snapshot.getOpen())
          .high(snapshot.getHigh())
          .low(snapshot.getLow())
          .close(snapshot.getClose())
          .volume(snapshot.getVolume())
          .source(snapshot.getSource())
          .createdAt(OffsetDateTime.now())
          .build();
    });

    QuoteSnapshotDto result = service.create(request);

    assertNull(result.volume());
  }

  @Test
  void create_handlesNullPriceFields() {
    OffsetDateTime now = OffsetDateTime.now();
    CreateQuoteSnapshotRequest request = new CreateQuoteSnapshotRequest(
        "PARTIAL",
        now,
        null, // open
        null, // high
        null, // low
        new BigDecimal("100.00"),
        1000L,
        "partial");

    UUID id = UUID.randomUUID();
    when(repository.save(any(QuoteSnapshot.class))).thenAnswer(invocation -> {
      QuoteSnapshot snapshot = invocation.getArgument(0);
      return QuoteSnapshot.builder()
          .id(id)
          .symbol(snapshot.getSymbol())
          .asOf(snapshot.getAsOf())
          .open(snapshot.getOpen())
          .high(snapshot.getHigh())
          .low(snapshot.getLow())
          .close(snapshot.getClose())
          .volume(snapshot.getVolume())
          .source(snapshot.getSource())
          .createdAt(OffsetDateTime.now())
          .build();
    });

    QuoteSnapshotDto result = service.create(request);

    assertNull(result.open());
    assertNull(result.high());
    assertNull(result.low());
    assertEquals(new BigDecimal("100.00"), result.close());
  }

  private QuoteSnapshot createSnapshot(UUID id, String symbol, OffsetDateTime asOf, String close) {
    return QuoteSnapshot.builder()
        .id(id)
        .symbol(symbol)
        .asOf(asOf)
        .open(new BigDecimal("100.00"))
        .high(new BigDecimal("105.00"))
        .low(new BigDecimal("98.00"))
        .close(new BigDecimal(close))
        .volume(10000L)
        .source("test")
        .createdAt(OffsetDateTime.now())
        .build();
  }
}
