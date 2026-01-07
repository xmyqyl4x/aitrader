package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.Position;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, UUID> {
  List<Position> findByAccountIdOrderByOpenedAtDesc(UUID accountId);

  List<Position> findByAccountIdAndClosedAtIsNullOrderByOpenedAtDesc(UUID accountId);

  Optional<Position> findByAccountIdAndSymbolAndClosedAtIsNull(UUID accountId, String symbol);
}
