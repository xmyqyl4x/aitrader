package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeLookupProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EtradeLookupProductRepository extends JpaRepository<EtradeLookupProduct, UUID> {
  Optional<EtradeLookupProduct> findBySymbolAndProductType(String symbol, String productType);
  List<EtradeLookupProduct> findBySymbol(String symbol);
  List<EtradeLookupProduct> findBySymbolOrderByLastSeenAtDesc(String symbol);
}
