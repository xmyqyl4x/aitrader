package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.BenchmarkDto;
import com.myqyl.aitradex.api.dto.CreateBenchmarkRequest;
import com.myqyl.aitradex.domain.Benchmark;
import com.myqyl.aitradex.repository.BenchmarkRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BenchmarkService {

  private final BenchmarkRepository benchmarkRepository;

  public BenchmarkService(BenchmarkRepository benchmarkRepository) {
    this.benchmarkRepository = benchmarkRepository;
  }

  @Transactional
  public BenchmarkDto create(CreateBenchmarkRequest request) {
    try {
      Benchmark benchmark =
          Benchmark.builder()
              .symbol(request.symbol().toUpperCase())
              .name(request.name())
              .build();
      return toDto(benchmarkRepository.save(benchmark));
    } catch (DataIntegrityViolationException e) {
      throw new DataIntegrityViolationException("Benchmark already exists: " + request.symbol(), e);
    }
  }

  @Transactional(readOnly = true)
  public List<BenchmarkDto> list() {
    return benchmarkRepository.findAll().stream().map(this::toDto).toList();
  }

  private BenchmarkDto toDto(Benchmark benchmark) {
    return new BenchmarkDto(benchmark.getId(), benchmark.getSymbol(), benchmark.getName());
  }
}
