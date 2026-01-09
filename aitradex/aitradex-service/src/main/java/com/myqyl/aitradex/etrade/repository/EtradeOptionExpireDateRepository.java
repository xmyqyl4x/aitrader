package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeOptionExpireDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EtradeOptionExpireDateRepository extends JpaRepository<EtradeOptionExpireDate, UUID> {
  Optional<EtradeOptionExpireDate> findBySymbolAndExpiryYearAndExpiryMonthAndExpiryDay(
      String symbol, Integer expiryYear, Integer expiryMonth, Integer expiryDay);
  List<EtradeOptionExpireDate> findBySymbolOrderByExpiryYearAscExpiryMonthAscExpiryDayAsc(String symbol);
}
