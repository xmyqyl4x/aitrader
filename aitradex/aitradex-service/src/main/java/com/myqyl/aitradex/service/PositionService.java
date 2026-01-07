package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreatePositionRequest;
import com.myqyl.aitradex.api.dto.PositionDto;
import com.myqyl.aitradex.api.dto.UpdatePositionCloseRequest;
import com.myqyl.aitradex.api.dto.UpdateStopLossRequest;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.Position;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.AccountRepository;
import com.myqyl.aitradex.repository.PositionRepository;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PositionService {

  private final PositionRepository positionRepository;
  private final AccountRepository accountRepository;

  public PositionService(PositionRepository positionRepository, AccountRepository accountRepository) {
    this.positionRepository = positionRepository;
    this.accountRepository = accountRepository;
  }

  @Transactional
  public PositionDto create(CreatePositionRequest request) {
    Account account =
        accountRepository.findById(request.accountId()).orElseThrow(() -> accountNotFound(request.accountId()));

    Position position =
        Position.builder()
            .account(account)
            .symbol(request.symbol().toUpperCase())
            .quantity(request.quantity().setScale(8, RoundingMode.HALF_UP))
            .costBasis(request.costBasis().setScale(8, RoundingMode.HALF_UP))
            .stopLoss(request.stopLoss())
            .openedAt(request.openedAt())
            .build();

    return toDto(positionRepository.save(position));
  }

  @Transactional
  public PositionDto updateStopLoss(UUID positionId, UpdateStopLossRequest request) {
    Position position =
        positionRepository.findById(positionId).orElseThrow(() -> positionNotFound(positionId));
    position.setStopLoss(request.stopLoss());
    return toDto(positionRepository.save(position));
  }

  @Transactional
  public PositionDto close(UUID positionId, UpdatePositionCloseRequest request) {
    Position position =
        positionRepository.findById(positionId).orElseThrow(() -> positionNotFound(positionId));
    position.setClosedAt(request.closedAt() != null ? request.closedAt() : OffsetDateTime.now());
    return toDto(positionRepository.save(position));
  }

  @Transactional(readOnly = true)
  public List<PositionDto> list(UUID accountId, boolean openOnly) {
    List<Position> positions;
    if (accountId != null && openOnly) {
      positions = positionRepository.findByAccountIdAndClosedAtIsNullOrderByOpenedAtDesc(accountId);
    } else if (accountId != null) {
      positions = positionRepository.findByAccountIdOrderByOpenedAtDesc(accountId);
    } else {
      positions = positionRepository.findAll();
    }
    return positions.stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public PositionDto get(UUID id) {
    return positionRepository.findById(id).map(this::toDto).orElseThrow(() -> positionNotFound(id));
  }

  private PositionDto toDto(Position position) {
    return new PositionDto(
        position.getId(),
        position.getAccount().getId(),
        position.getSymbol(),
        position.getQuantity(),
        position.getCostBasis(),
        position.getStopLoss(),
        position.getOpenedAt(),
        position.getClosedAt(),
        position.getCreatedAt(),
        position.getUpdatedAt());
  }

  private NotFoundException positionNotFound(UUID id) {
    return new NotFoundException("Position %s not found".formatted(id));
  }

  private NotFoundException accountNotFound(UUID id) {
    return new NotFoundException("Account %s not found".formatted(id));
  }
}
