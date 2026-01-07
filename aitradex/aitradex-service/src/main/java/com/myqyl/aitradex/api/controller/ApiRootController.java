package com.myqyl.aitradex.api.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiRootController {

  @GetMapping
  public Map<String, String> apiRoot() {
    return Map.of(
        "status", "ok",
        "message", "Aitradex API is running");
  }
}
