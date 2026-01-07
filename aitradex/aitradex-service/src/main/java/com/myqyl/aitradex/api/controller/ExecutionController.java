package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.CreateExecutionRequest;
import com.myqyl.aitradex.api.dto.ExecutionDto;
import com.myqyl.aitradex.service.ExecutionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

  private final ExecutionService executionService;

  public ExecutionController(ExecutionService executionService) {
    this.executionService = executionService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ExecutionDto create(@Valid @RequestBody CreateExecutionRequest request) {
    return executionService.create(request);
  }

  @GetMapping
  public List<ExecutionDto> list() {
    return executionService.list();
  }

  @GetMapping("/{id}")
  public ExecutionDto get(@PathVariable UUID id) {
    return executionService.get(id);
  }
}
