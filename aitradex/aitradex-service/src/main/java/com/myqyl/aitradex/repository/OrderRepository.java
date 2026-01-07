package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.Order;
import com.myqyl.aitradex.domain.OrderStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
  List<Order> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

  List<Order> findByAccountIdAndStatusOrderByCreatedAtDesc(UUID accountId, OrderStatus status);

  List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

  boolean existsByAccountIdAndSymbolAndStatusIn(UUID accountId, String symbol, List<OrderStatus> statuses);
}
