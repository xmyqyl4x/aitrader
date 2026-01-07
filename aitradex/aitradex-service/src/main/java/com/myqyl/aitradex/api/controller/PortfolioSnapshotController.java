package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.CreatePortfolioSnapshotRequest;
import com.myqyl.aitradex.api.dto.PortfolioSnapshotDto;
import com.myqyl.aitradex.service.PortfolioSnapshotService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio-snapshots")
public class PortfolioSnapshotController {

  private final PortfolioSnapshotService snapshotService;

  public PortfolioSnapshotController(PortfolioSnapshotService snapshotService) {
    this.snapshotService = snapshotService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PortfolioSnapshotDto create(@Valid @RequestBody CreatePortfolioSnapshotRequest request) {
    return snapshotService.create(request);
  }

  @GetMapping
  public List<PortfolioSnapshotDto> list(
      @RequestParam(value = "accountId", required = false) UUID accountId,
      @RequestParam(value = "startDate", required = false) LocalDate startDate,
      @RequestParam(value = "endDate", required = false) LocalDate endDate) {
    return snapshotService.list(accountId, startDate, endDate);
  }

  @GetMapping("/{id}")
  public PortfolioSnapshotDto get(@PathVariable UUID id) {
    return snapshotService.get(id);
  }
}
