package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.AnalyticsSummaryDto;
import com.myqyl.aitradex.service.AnalyticsService;
import jakarta.validation.constraints.NotNull;
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
}
