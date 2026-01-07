package com.myqyl.aitradex.marketdata;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.domain.QuoteSnapshot;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.QuoteSnapshotRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QuoteSnapshotAdapter implements MarketDataAdapter {

  private final QuoteSnapshotRepository quoteSnapshotRepository;

  public QuoteSnapshotAdapter(QuoteSnapshotRepository quoteSnapshotRepository) {
    this.quoteSnapshotRepository = quoteSnapshotRepository;
  }

  @Override
  public String name() {
    return "quote-snapshots";
  }

  @Override
  public MarketDataQuoteDto latestQuote(String symbol) {
    List<QuoteSnapshot> snapshots =
        quoteSnapshotRepository.findBySymbolOrderByAsOfDesc(symbol.toUpperCase());
    if (snapshots.isEmpty()) {
      throw new NotFoundException("Quote snapshot for %s not found".formatted(symbol));
    }
    QuoteSnapshot snapshot = snapshots.get(0);
    return new MarketDataQuoteDto(
        snapshot.getSymbol(),
        snapshot.getAsOf(),
        snapshot.getOpen(),
        snapshot.getHigh(),
        snapshot.getLow(),
        snapshot.getClose(),
        snapshot.getVolume(),
        snapshot.getSource());
  }
}
