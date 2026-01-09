package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeQuoteSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EtradeQuoteSnapshotRepository extends JpaRepository<EtradeQuoteSnapshot, UUID> {
  List<EtradeQuoteSnapshot> findBySymbolOrderByRequestTimeDesc(String symbol);
  long countBySymbol(String symbol);
}
