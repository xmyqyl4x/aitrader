package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreateTradeLogRequest;
import com.myqyl.aitradex.api.dto.TradeLogDto;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.TradeLog;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.AccountRepository;
import com.myqyl.aitradex.repository.TradeLogRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeLogService {

  private final TradeLogRepository tradeLogRepository;
  private final AccountRepository accountRepository;

  public TradeLogService(
      TradeLogRepository tradeLogRepository, AccountRepository accountRepository) {
    this.tradeLogRepository = tradeLogRepository;
    this.accountRepository = accountRepository;
  }

  @Transactional
  public TradeLogDto create(CreateTradeLogRequest request) {
    Account account =
        accountRepository.findById(request.accountId()).orElseThrow(() -> accountNotFound(request.accountId()));

    TradeLog log =
        TradeLog.builder()
            .account(account)
            .action(request.action())
            .reason(request.reason())
            .metadata(request.metadata())
            .occurredAt(OffsetDateTime.now())
            .build();

    return toDto(tradeLogRepository.save(log));
  }

  @Transactional(readOnly = true)
  public List<TradeLogDto> list(UUID accountId) {
    List<TradeLog> logs =
        accountId != null
            ? tradeLogRepository.findByAccountIdOrderByOccurredAtDesc(accountId)
            : tradeLogRepository.findAllByOrderByOccurredAtDesc();
    return logs.stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public TradeLogDto get(UUID id) {
    return tradeLogRepository.findById(id).map(this::toDto).orElseThrow(() -> tradeLogNotFound(id));
  }

  private TradeLogDto toDto(TradeLog log) {
    return new TradeLogDto(
        log.getId(),
        log.getAccount().getId(),
        log.getAction(),
        log.getReason(),
        log.getMetadata(),
        log.getOccurredAt());
  }

  private NotFoundException tradeLogNotFound(UUID id) {
    return new NotFoundException("Trade log %s not found".formatted(id));
  }

  private NotFoundException accountNotFound(UUID id) {
    return new NotFoundException("Account %s not found".formatted(id));
  }
}
