package com.myqyl.aitradex.marketdata;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;

public interface MarketDataAdapter {

  String name();

  MarketDataQuoteDto latestQuote(String symbol);
}
