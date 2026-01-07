package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreateQuoteSnapshotRequest;
import com.myqyl.aitradex.api.dto.QuoteSnapshotDto;
import com.myqyl.aitradex.domain.QuoteSnapshot;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.QuoteSnapshotRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteSnapshotService {

  private final QuoteSnapshotRepository quoteSnapshotRepository;

  public QuoteSnapshotService(QuoteSnapshotRepository quoteSnapshotRepository) {
    this.quoteSnapshotRepository = quoteSnapshotRepository;
  }

  @Transactional
  public QuoteSnapshotDto create(CreateQuoteSnapshotRequest request) {
    QuoteSnapshot snapshot =
        QuoteSnapshot.builder()
            .symbol(request.symbol().toUpperCase())
            .asOf(request.asOf())
            .open(request.open())
            .high(request.high())
            .low(request.low())
            .close(request.close())
            .volume(request.volume())
            .source(request.source())
            .build();
    return toDto(quoteSnapshotRepository.save(snapshot));
  }

  @Transactional(readOnly = true)
  public List<QuoteSnapshotDto> list(String symbol) {
    List<QuoteSnapshot> snapshots =
        symbol != null
            ? quoteSnapshotRepository.findBySymbolOrderByAsOfDesc(symbol.toUpperCase())
            : quoteSnapshotRepository.findTop100ByOrderByAsOfDesc();
    return snapshots.stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public QuoteSnapshotDto get(UUID id) {
    return quoteSnapshotRepository.findById(id).map(this::toDto).orElseThrow(() -> snapshotNotFound(id));
  }

  private QuoteSnapshotDto toDto(QuoteSnapshot snapshot) {
    return new QuoteSnapshotDto(
        snapshot.getId(),
        snapshot.getSymbol(),
        snapshot.getAsOf(),
        snapshot.getOpen(),
        snapshot.getHigh(),
        snapshot.getLow(),
        snapshot.getClose(),
        snapshot.getVolume(),
        snapshot.getSource(),
        snapshot.getCreatedAt());
  }

  private NotFoundException snapshotNotFound(UUID id) {
    return new NotFoundException("Quote snapshot %s not found".formatted(id));
  }
}
