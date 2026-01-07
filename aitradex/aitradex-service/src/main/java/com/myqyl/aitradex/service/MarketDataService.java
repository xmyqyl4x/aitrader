package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.marketdata.MarketDataAdapter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

  private final List<MarketDataAdapter> adapters;

  public MarketDataService(List<MarketDataAdapter> adapters) {
    this.adapters = adapters;
  }

  public MarketDataQuoteDto latestQuote(String symbol, String source) {
    return resolveAdapter(source).latestQuote(symbol);
  }

  public List<String> listSources() {
    return adapters.stream().map(MarketDataAdapter::name).toList();
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
