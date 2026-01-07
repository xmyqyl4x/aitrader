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

  public MarketDataQuoteDto latestQuote(String symbol) {
    return defaultAdapter().latestQuote(symbol);
  }

  public List<String> listSources() {
    return adapters.stream().map(MarketDataAdapter::name).toList();
  }

  private MarketDataAdapter defaultAdapter() {
    return adapters.get(0);
  }
}
