package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.AnalyticsSummaryDto;
import com.myqyl.aitradex.domain.PortfolioSnapshot;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.AccountRepository;
import com.myqyl.aitradex.repository.PortfolioSnapshotRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

  private static final MathContext MATH_CONTEXT = new MathContext(8, RoundingMode.HALF_UP);

  private final PortfolioSnapshotRepository snapshotRepository;
  private final AccountRepository accountRepository;

  public AnalyticsService(
      PortfolioSnapshotRepository snapshotRepository, AccountRepository accountRepository) {
    this.snapshotRepository = snapshotRepository;
    this.accountRepository = accountRepository;
  }

  @Transactional(readOnly = true)
  public AnalyticsSummaryDto summarizeAccount(UUID accountId) {
    accountRepository.findById(accountId).orElseThrow(() -> accountNotFound(accountId));

    List<PortfolioSnapshot> snapshots =
        snapshotRepository.findByAccountIdOrderByAsOfDateAsc(accountId);
    if (snapshots.isEmpty()) {
      throw new NotFoundException(
          "No portfolio snapshots found for account %s".formatted(accountId));
    }

    PortfolioSnapshot first = snapshots.get(0);
    PortfolioSnapshot last = snapshots.get(snapshots.size() - 1);

    BigDecimal startingEquity = first.getEquity();
    BigDecimal endingEquity = last.getEquity();
    BigDecimal absolutePnl = endingEquity.subtract(startingEquity, MATH_CONTEXT);
    BigDecimal returnPct =
        startingEquity.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : absolutePnl.divide(startingEquity, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    BigDecimal maxDrawdown = calculateMaxDrawdown(snapshots);

    return new AnalyticsSummaryDto(
        accountId,
        first.getAsOfDate(),
        last.getAsOfDate(),
        startingEquity,
        endingEquity,
        absolutePnl,
        returnPct,
        maxDrawdown);
  }

  private BigDecimal calculateMaxDrawdown(List<PortfolioSnapshot> snapshots) {
    BigDecimal peak = BigDecimal.ZERO;
    BigDecimal maxDrawdown = BigDecimal.ZERO;

    for (PortfolioSnapshot snapshot : snapshots) {
      BigDecimal equity = snapshot.getEquity();
      if (equity.compareTo(peak) > 0) {
        peak = equity;
      }
      if (peak.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal drawdown =
            equity.subtract(peak).divide(peak, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        if (drawdown.compareTo(maxDrawdown) < 0) {
          maxDrawdown = drawdown;
        }
      }
    }
    return maxDrawdown;
  }

  private NotFoundException accountNotFound(UUID id) {
    return new NotFoundException("Account %s not found".formatted(id));
  }
}
