package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.AccountDto;
import com.myqyl.aitradex.api.dto.CreateAccountRequest;
import com.myqyl.aitradex.service.AccountService;
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
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AccountDto createAccount(@Valid @RequestBody CreateAccountRequest request) {
    return accountService.create(request);
  }

  @GetMapping
  public List<AccountDto> listAccounts() {
    return accountService.list();
  }

  @GetMapping("/{id}")
  public AccountDto getAccount(@PathVariable UUID id) {
    return accountService.get(id);
  }
}
