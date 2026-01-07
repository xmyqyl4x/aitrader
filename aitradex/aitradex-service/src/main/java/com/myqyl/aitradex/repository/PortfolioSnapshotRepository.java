package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.PortfolioSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {

  List<PortfolioSnapshot> findByAccountIdOrderByAsOfDateDesc(UUID accountId);

  List<PortfolioSnapshot> findByAccountIdOrderByAsOfDateAsc(UUID accountId);
}
