package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.Execution;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRepository extends JpaRepository<Execution, UUID> {
  List<Execution> findByOrderIdOrderByExecutedAtDesc(UUID orderId);
}
