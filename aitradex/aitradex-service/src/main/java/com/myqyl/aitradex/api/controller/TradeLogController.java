package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.CreateTradeLogRequest;
import com.myqyl.aitradex.api.dto.TradeLogDto;
import com.myqyl.aitradex.service.TradeLogService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/trade-logs")
public class TradeLogController {

  private final TradeLogService tradeLogService;

  public TradeLogController(TradeLogService tradeLogService) {
    this.tradeLogService = tradeLogService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TradeLogDto create(@Valid @RequestBody CreateTradeLogRequest request) {
    return tradeLogService.create(request);
  }

  @GetMapping
  public List<TradeLogDto> list(
      @RequestParam(value = "accountId", required = false) UUID accountId) {
    return tradeLogService.list(accountId);
  }

  @GetMapping("/{id}")
  public TradeLogDto get(@PathVariable UUID id) {
    return tradeLogService.get(id);
  }
}
