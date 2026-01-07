package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.marketdata.MarketDataAdapter;
import com.myqyl.aitradex.repository.QuoteSnapshotRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

  private final List<MarketDataAdapter> adapters;
  private final QuoteSnapshotRepository quoteSnapshotRepository;

  public MarketDataService(
      List<MarketDataAdapter> adapters, QuoteSnapshotRepository quoteSnapshotRepository) {
    this.adapters = adapters;
    this.quoteSnapshotRepository = quoteSnapshotRepository;
  }

  public MarketDataQuoteDto latestQuote(String symbol, String source) {
    return resolveAdapter(source).latestQuote(symbol);
  }

  public List<String> listSources() {
    return adapters.stream().map(MarketDataAdapter::name).toList();
  }

  public Map<String, Long> sourceCounts() {
    return adapters.stream()
        .collect(Collectors.toMap(
            MarketDataAdapter::name,
            name -> quoteSnapshotRepository.countBySource(name)));
  }

  private MarketDataAdapter resolveAdapter(String source) {
    if (source == null || source.isBlank()) {
      return adapters.get(0);
    }
    return adapters.stream()
        .filter(adapter -> adapter.name().equalsIgnoreCase(source))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown market data source: " + source));
  }
}
