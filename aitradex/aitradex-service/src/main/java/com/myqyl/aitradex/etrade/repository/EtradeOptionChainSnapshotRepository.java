package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeOptionChainSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EtradeOptionChainSnapshotRepository extends JpaRepository<EtradeOptionChainSnapshot, UUID> {
  List<EtradeOptionChainSnapshot> findBySymbolOrderByRequestTimeDesc(String symbol);
  List<EtradeOptionChainSnapshot> findBySymbolAndExpiryYearAndExpiryMonthAndExpiryDayOrderByRequestTimeDesc(
      String symbol, Integer expiryYear, Integer expiryMonth, Integer expiryDay);
}
