package com.myqyl.aitradex.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketDataCacheScheduler {

  private final MarketDataService marketDataService;

  public MarketDataCacheScheduler(MarketDataService marketDataService) {
    this.marketDataService = marketDataService;
  }

  @Scheduled(fixedDelayString = "${app.market-data.cache-evict-interval-ms:300000}")
  public void evictExpiredEntries() {
    marketDataService.purgeExpired();
  }
}
