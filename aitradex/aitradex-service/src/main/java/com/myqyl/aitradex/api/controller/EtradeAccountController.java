package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.EtradeAccountDto;
import com.myqyl.aitradex.etrade.service.EtradeAccountService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for E*TRADE account operations.
 */
@RestController
@RequestMapping("/api/etrade/accounts")
@ConditionalOnProperty(name = "app.etrade.enabled", havingValue = "true", matchIfMissing = false)
public class EtradeAccountController {

  private static final Logger log = LoggerFactory.getLogger(EtradeAccountController.class);

  private final EtradeAccountService accountService;

  public EtradeAccountController(EtradeAccountService accountService) {
    this.accountService = accountService;
  }

  /**
   * Gets all linked accounts for a user.
   */
  @GetMapping
  public ResponseEntity<List<EtradeAccountDto>> getUserAccounts(@RequestParam(required = false) UUID userId) {
    if (userId == null) {
      userId = UUID.randomUUID(); // For MVP
    }
    
    List<EtradeAccountDto> accounts = accountService.getUserAccounts(userId);
    return ResponseEntity.ok(accounts);
  }

  /**
   * Gets account details.
   */
  @GetMapping("/{accountId}")
  public ResponseEntity<EtradeAccountDto> getAccount(@PathVariable UUID accountId) {
    EtradeAccountDto account = accountService.getAccount(accountId);
    return ResponseEntity.ok(account);
  }

  /**
   * Gets account balance.
   */
  @GetMapping("/{accountId}/balance")
  public ResponseEntity<Map<String, Object>> getBalance(@PathVariable UUID accountId) {
    EtradeAccountDto account = accountService.getAccount(accountId);
    Map<String, Object> balance = accountService.getAccountBalance(accountId, account.accountIdKey());
    return ResponseEntity.ok(balance);
  }

  /**
   * Gets account portfolio.
   */
  @GetMapping("/{accountId}/portfolio")
  public ResponseEntity<Map<String, Object>> getPortfolio(@PathVariable UUID accountId) {
    EtradeAccountDto account = accountService.getAccount(accountId);
    Map<String, Object> portfolio = accountService.getAccountPortfolio(accountId, account.accountIdKey());
    return ResponseEntity.ok(portfolio);
  }

  /**
   * Syncs accounts from E*TRADE.
   */
  @PostMapping("/sync")
  public ResponseEntity<List<EtradeAccountDto>> syncAccounts(
      @RequestParam(required = false) UUID userId,
      @RequestParam UUID accountId) {
    if (userId == null) {
      userId = UUID.randomUUID(); // For MVP
    }
    
    List<EtradeAccountDto> accounts = accountService.syncAccounts(userId, accountId);
    return ResponseEntity.ok(accounts);
  }

  /**
   * Unlinks an account.
   */
  @DeleteMapping("/{accountId}")
  public ResponseEntity<Void> unlinkAccount(@PathVariable UUID accountId) {
    accountService.unlinkAccount(accountId);
    return ResponseEntity.noContent().build();
  }
}
