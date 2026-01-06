package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.CreateQuoteSnapshotRequest;
import com.myqyl.aitradex.api.dto.QuoteSnapshotDto;
import com.myqyl.aitradex.service.QuoteSnapshotService;
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
@RequestMapping("/api/quotes")
public class QuoteSnapshotController {

  private final QuoteSnapshotService quoteSnapshotService;

  public QuoteSnapshotController(QuoteSnapshotService quoteSnapshotService) {
    this.quoteSnapshotService = quoteSnapshotService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public QuoteSnapshotDto create(@Valid @RequestBody CreateQuoteSnapshotRequest request) {
    return quoteSnapshotService.create(request);
  }

  @GetMapping
  public List<QuoteSnapshotDto> list(
      @RequestParam(value = "symbol", required = false) String symbol) {
    return quoteSnapshotService.list(symbol);
  }

  @GetMapping("/{id}")
  public QuoteSnapshotDto get(@PathVariable UUID id) {
    return quoteSnapshotService.get(id);
  }
}
