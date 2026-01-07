package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.Benchmark;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenchmarkRepository extends JpaRepository<Benchmark, UUID> {

  Optional<Benchmark> findBySymbol(String symbol);
}
