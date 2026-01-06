package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.CreatePositionRequest;
import com.myqyl.aitradex.api.dto.PositionDto;
import com.myqyl.aitradex.api.dto.UpdateStopLossRequest;
import com.myqyl.aitradex.service.PositionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

  private final PositionService positionService;

  public PositionController(PositionService positionService) {
    this.positionService = positionService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PositionDto create(@Valid @RequestBody CreatePositionRequest request) {
    return positionService.create(request);
  }

  @GetMapping
  public List<PositionDto> list() {
    return positionService.list();
  }

  @GetMapping("/{id}")
  public PositionDto get(@PathVariable UUID id) {
    return positionService.get(id);
  }

  @PatchMapping("/{id}/stop-loss")
  public PositionDto updateStopLoss(
      @PathVariable UUID id, @Valid @RequestBody UpdateStopLossRequest request) {
    return positionService.updateStopLoss(id, request);
  }
}
