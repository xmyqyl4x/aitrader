package com.myqyl.aitradex.service;

import com.myqyl.aitradex.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PortfolioSnapshotScheduler {

  private final AccountRepository accountRepository;
  private final PortfolioSnapshotService snapshotService;
  private final String source;

  public PortfolioSnapshotScheduler(
      AccountRepository accountRepository,
      PortfolioSnapshotService snapshotService,
      @Value("${app.market-data.default-source:quote-snapshots}") String source) {
    this.accountRepository = accountRepository;
    this.snapshotService = snapshotService;
    this.source = source;
  }

  @Scheduled(fixedDelayString = "${app.snapshots.poll-interval-ms:300000}")
  public void captureSnapshots() {
    accountRepository.findAll()
        .forEach(account -> snapshotService.createSnapshotForAccount(account.getId(), source));
  }
}
