package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.AnalyticsSummaryDto;
import com.myqyl.aitradex.api.dto.EquityPointDto;
import com.myqyl.aitradex.api.dto.SymbolPnlDto;
import com.myqyl.aitradex.domain.PortfolioSnapshot;
import com.myqyl.aitradex.domain.Position;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.AccountRepository;
import com.myqyl.aitradex.repository.PortfolioSnapshotRepository;
import com.myqyl.aitradex.repository.PositionRepository;
import com.myqyl.aitradex.util.PriceUtils;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

  private static final MathContext MATH_CONTEXT = new MathContext(8, RoundingMode.HALF_UP);

  private final PortfolioSnapshotRepository snapshotRepository;
  private final AccountRepository accountRepository;
  private final PositionRepository positionRepository;
  private final MarketDataService marketDataService;

  public AnalyticsService(
      PortfolioSnapshotRepository snapshotRepository,
      AccountRepository accountRepository,
      PositionRepository positionRepository,
      MarketDataService marketDataService) {
    this.snapshotRepository = snapshotRepository;
    this.accountRepository = accountRepository;
    this.positionRepository = positionRepository;
    this.marketDataService = marketDataService;
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

  @Transactional(readOnly = true)
  public List<EquityPointDto> equityCurve(UUID accountId, LocalDate startDate, LocalDate endDate) {
    accountRepository.findById(accountId).orElseThrow(() -> accountNotFound(accountId));
    List<PortfolioSnapshot> snapshots =
        snapshotRepository.findByAccountIdAndAsOfDateBetweenOrderByAsOfDateAsc(
            accountId,
            startDate != null ? startDate : LocalDate.of(1970, 1, 1),
            endDate != null ? endDate : LocalDate.of(2999, 12, 31));
    return snapshots.stream()
        .map(snapshot -> new EquityPointDto(snapshot.getAsOfDate(), snapshot.getEquity(), snapshot.getDrawdown()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SymbolPnlDto> symbolPnl(UUID accountId, String source) {
    accountRepository.findById(accountId).orElseThrow(() -> accountNotFound(accountId));
    List<Position> positions =
        positionRepository.findByAccountIdAndClosedAtIsNullOrderByOpenedAtDesc(accountId);

    return positions.stream()
        .map(
            position -> {
              var quote = marketDataService.latestQuote(position.getSymbol(), source);
              BigDecimal lastPrice = null;
              if (quote != null) {
                lastPrice = PriceUtils.firstAvailable(quote.close(), quote.open(), quote.high(), quote.low());
              }
              BigDecimal pnl =
                  lastPrice != null && position.getCostBasis() != null
                      ? lastPrice.subtract(position.getCostBasis()).multiply(position.getQuantity())
                      : null;
              return new SymbolPnlDto(
                  position.getSymbol(),
                  position.getQuantity(),
                  position.getCostBasis(),
                  lastPrice,
                  pnl);
            })
        .sorted(Comparator.comparing(SymbolPnlDto::symbol))
        .toList();
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
