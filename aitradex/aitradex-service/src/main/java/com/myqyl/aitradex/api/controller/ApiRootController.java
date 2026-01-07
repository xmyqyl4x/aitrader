package com.myqyl.aitradex.api.controller;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiRootController {

  @GetMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, String> apiRoot() {
    return Map.of("status", "ok");
  }
}
