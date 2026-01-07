package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.service.StopLossService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

  private final StopLossService stopLossService;

  public RiskController(StopLossService stopLossService) {
    this.stopLossService = stopLossService;
  }

  @PostMapping("/stop-loss/run")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public int runStopLosses(@RequestParam(value = "source", required = false) String source) {
    return stopLossService.enforceStopLosses(source);
  }
}
