package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.BenchmarkDto;
import com.myqyl.aitradex.api.dto.CreateBenchmarkRequest;
import com.myqyl.aitradex.service.BenchmarkService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmarks")
public class BenchmarkController {

  private final BenchmarkService benchmarkService;

  public BenchmarkController(BenchmarkService benchmarkService) {
    this.benchmarkService = benchmarkService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BenchmarkDto createBenchmark(@Valid @RequestBody CreateBenchmarkRequest request) {
    return benchmarkService.create(request);
  }

  @GetMapping
  public List<BenchmarkDto> listBenchmarks() {
    return benchmarkService.list();
  }
}
