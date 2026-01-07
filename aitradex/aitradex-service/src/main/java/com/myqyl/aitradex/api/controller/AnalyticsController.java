package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.AnalyticsSummaryDto;
import com.myqyl.aitradex.api.dto.EquityPointDto;
import com.myqyl.aitradex.api.dto.SymbolPnlDto;
import com.myqyl.aitradex.service.AnalyticsService;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  public AnalyticsController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @GetMapping("/summary")
  public AnalyticsSummaryDto summary(@RequestParam("accountId") @NotNull UUID accountId) {
    return analyticsService.summarizeAccount(accountId);
  }

  @GetMapping("/equity")
  public List<EquityPointDto> equity(
      @RequestParam("accountId") @NotNull UUID accountId,
      @RequestParam(value = "startDate", required = false) LocalDate startDate,
      @RequestParam(value = "endDate", required = false) LocalDate endDate) {
    return analyticsService.equityCurve(accountId, startDate, endDate);
  }

  @GetMapping("/pnl")
  public List<SymbolPnlDto> symbolPnl(
      @RequestParam("accountId") @NotNull UUID accountId,
      @RequestParam(value = "source", required = false) String source) {
    return analyticsService.symbolPnl(accountId, source);
  }
}
