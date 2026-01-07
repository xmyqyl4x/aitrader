package com.myqyl.aitradex.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StopLossScheduler {

  private final StopLossService stopLossService;
  private final String source;

  public StopLossScheduler(
      StopLossService stopLossService,
      @Value("${app.stop-loss.source:quote-snapshots}") String source) {
    this.stopLossService = stopLossService;
    this.source = source;
  }

  @Scheduled(fixedDelayString = "${app.stop-loss.poll-interval-ms:60000}")
  public void pollStopLosses() {
    stopLossService.enforceStopLosses(source);
  }
}
