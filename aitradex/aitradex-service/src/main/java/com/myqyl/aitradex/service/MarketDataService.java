package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.config.MarketDataProperties;
import com.myqyl.aitradex.marketdata.MarketDataAdapter;
import com.myqyl.aitradex.repository.QuoteSnapshotRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

  private final List<MarketDataAdapter> adapters;
  private final QuoteSnapshotRepository quoteSnapshotRepository;
  private final MarketDataProperties properties;
  private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

  public MarketDataService(
      List<MarketDataAdapter> adapters,
      QuoteSnapshotRepository quoteSnapshotRepository,
      MarketDataProperties properties) {
    this.adapters = adapters;
    this.quoteSnapshotRepository = quoteSnapshotRepository;
    this.properties = properties;
  }

  public MarketDataQuoteDto latestQuote(String symbol, String source) {
    String resolvedSource = resolveSource(source);
    String cacheKey = resolvedSource + ":" + symbol.toUpperCase();
    CacheEntry cached = cache.get(cacheKey);
    if (cached != null && !cached.isExpired()) {
      return cached.quote();
    }

    MarketDataQuoteDto quote = resolveAdapter(resolvedSource).latestQuote(symbol);
    cache.put(cacheKey, new CacheEntry(quote, Instant.now().plus(properties.getCacheTtl())));
    return quote;
  }

  public MarketDataQuoteDto latestQuote(String symbol) {
    return latestQuote(symbol, properties.getDefaultSource());
  }

  public List<String> listSources() {
    return adapters.stream().map(MarketDataAdapter::name).toList();
  }

  public Map<String, Long> sourceCounts() {
    return adapters.stream()
        .collect(Collectors.toMap(
            MarketDataAdapter::name,
            adapter -> {
              String name = adapter.name();
              return "quote-snapshots".equalsIgnoreCase(name)
                  ? quoteSnapshotRepository.countBySource(name)
                  : 0L;
            }));
  }

  public void purgeExpired() {
    cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
  }

  private String resolveSource(String source) {
    if (source == null || source.isBlank()) {
      return properties.getDefaultSource();
    }
    return source;
  }

  private MarketDataAdapter resolveAdapter(String source) {
    return adapters.stream()
        .sorted(Comparator.comparing(MarketDataAdapter::name))
        .filter(adapter -> adapter.name().equalsIgnoreCase(source))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown market data source: " + source));
  }

  private record CacheEntry(MarketDataQuoteDto quote, Instant expiresAt) {
    boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }
  }
}
