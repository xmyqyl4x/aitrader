package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EtradeOrderRepository extends JpaRepository<EtradeOrder, UUID> {
  
  List<EtradeOrder> findByAccountId(UUID accountId);
  
  Page<EtradeOrder> findByAccountId(UUID accountId, Pageable pageable);
  
  Optional<EtradeOrder> findByEtradeOrderId(String etradeOrderId);
  
  List<EtradeOrder> findByAccountIdAndOrderStatus(UUID accountId, String orderStatus);
  
  List<EtradeOrder> findBySymbol(String symbol);
}
