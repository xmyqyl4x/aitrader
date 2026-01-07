package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreatePortfolioSnapshotRequest;
import com.myqyl.aitradex.api.dto.PortfolioSnapshotDto;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.PortfolioSnapshot;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.AccountRepository;
import com.myqyl.aitradex.repository.PortfolioSnapshotRepository;
import java.math.RoundingMode;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioSnapshotService {

  private final PortfolioSnapshotRepository snapshotRepository;
  private final AccountRepository accountRepository;

  public PortfolioSnapshotService(
      PortfolioSnapshotRepository snapshotRepository, AccountRepository accountRepository) {
    this.snapshotRepository = snapshotRepository;
    this.accountRepository = accountRepository;
  }

  @Transactional
  public PortfolioSnapshotDto create(CreatePortfolioSnapshotRequest request) {
    Account account =
        accountRepository.findById(request.accountId()).orElseThrow(() -> accountNotFound(request.accountId()));

    PortfolioSnapshot snapshot =
        PortfolioSnapshot.builder()
            .account(account)
            .asOfDate(request.asOfDate())
            .equity(request.equity().setScale(4, RoundingMode.HALF_UP))
            .cash(request.cash().setScale(4, RoundingMode.HALF_UP))
            .pnl(request.pnl() != null ? request.pnl().setScale(4, RoundingMode.HALF_UP) : null)
            .drawdown(
                request.drawdown() != null
                    ? request.drawdown().setScale(4, RoundingMode.HALF_UP)
                    : null)
            .build();

    return toDto(snapshotRepository.save(snapshot));
  }

  @Transactional(readOnly = true)
  public List<PortfolioSnapshotDto> list(UUID accountId, LocalDate startDate, LocalDate endDate) {
    if (accountId != null) {
      if (startDate != null && endDate != null) {
        return snapshotRepository
            .findByAccountIdAndAsOfDateBetweenOrderByAsOfDateAsc(accountId, startDate, endDate)
            .stream()
            .map(this::toDto)
            .toList();
      }
      return snapshotRepository.findByAccountIdOrderByAsOfDateAsc(accountId).stream()
          .map(this::toDto)
          .toList();
    }
    return snapshotRepository.findAll().stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public PortfolioSnapshotDto get(UUID id) {
    return snapshotRepository.findById(id).map(this::toDto).orElseThrow(() -> snapshotNotFound(id));
  }

  private PortfolioSnapshotDto toDto(PortfolioSnapshot snapshot) {
    return new PortfolioSnapshotDto(
        snapshot.getId(),
        snapshot.getAccount().getId(),
        snapshot.getAsOfDate(),
        snapshot.getEquity(),
        snapshot.getCash(),
        snapshot.getPnl(),
        snapshot.getDrawdown(),
        snapshot.getCreatedAt());
  }

  private NotFoundException snapshotNotFound(UUID id) {
    return new NotFoundException("Portfolio snapshot %s not found".formatted(id));
  }

  private NotFoundException accountNotFound(UUID id) {
    return new NotFoundException("Account %s not found".formatted(id));
  }
}
