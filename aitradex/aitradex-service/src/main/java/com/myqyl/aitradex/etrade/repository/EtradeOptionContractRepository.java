package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeOptionContract;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EtradeOptionContractRepository extends JpaRepository<EtradeOptionContract, UUID> {
  Optional<EtradeOptionContract> findByOptionSymbol(String optionSymbol);
  Optional<EtradeOptionContract> findByOsiKey(String osiKey);
  List<EtradeOptionContract> findByUnderlyingSymbol(String underlyingSymbol);
  List<EtradeOptionContract> findByUnderlyingSymbolAndExpiryYearAndExpiryMonthAndExpiryDay(
      String underlyingSymbol, Integer expiryYear, Integer expiryMonth, Integer expiryDay);
}
