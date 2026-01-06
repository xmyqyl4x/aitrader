package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.PortfolioSnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {
}
