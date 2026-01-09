package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.EtradeAccountDto;
import com.myqyl.aitradex.etrade.accounts.dto.BalanceResponse;
import com.myqyl.aitradex.etrade.accounts.dto.PortfolioResponse;
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
   * 
   * @return BalanceResponse DTO
   */
  @GetMapping("/{accountId}/balance")
  public ResponseEntity<BalanceResponse> getBalance(
      @PathVariable UUID accountId,
      @RequestParam(required = false) String instType,
      @RequestParam(required = false) String accountType,
      @RequestParam(required = false) Boolean realTimeNAV) {
    EtradeAccountDto account = accountService.getAccount(accountId);
    BalanceResponse balance = accountService.getAccountBalance(accountId, account.accountIdKey(), 
                                                               instType, accountType, realTimeNAV);
    return ResponseEntity.ok(balance);
  }

  /**
   * Gets account portfolio.
   * 
   * @return PortfolioResponse DTO
   */
  @GetMapping("/{accountId}/portfolio")
  public ResponseEntity<PortfolioResponse> getPortfolio(
      @PathVariable UUID accountId,
      @RequestParam(required = false) Integer count,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortOrder,
      @RequestParam(required = false) Integer pageNumber,
      @RequestParam(required = false) String marketSession,
      @RequestParam(required = false) Boolean totalsRequired,
      @RequestParam(required = false) Boolean lotsRequired,
      @RequestParam(required = false) String view) {
    EtradeAccountDto account = accountService.getAccount(accountId);
    PortfolioResponse portfolio = accountService.getAccountPortfolio(accountId, account.accountIdKey(),
                                                                     count, sortBy, sortOrder, pageNumber,
                                                                     marketSession, totalsRequired, lotsRequired, view);
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
   * Gets account transactions.
   */
  @GetMapping("/{accountId}/transactions")
  public ResponseEntity<Map<String, Object>> getTransactions(
      @PathVariable UUID accountId,
      @RequestParam(required = false) String marker,
      @RequestParam(required = false) Integer count,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(required = false) String sortOrder,
      @RequestParam(required = false) String accept,
      @RequestParam(required = false) String storeId) {
    Map<String, Object> result = accountService.getAccountTransactions(accountId, marker, count,
                                                                        startDate, endDate, sortOrder, accept, storeId);
    return ResponseEntity.ok(result);
  }

  /**
   * Gets transaction details.
   */
  @GetMapping("/{accountId}/transactions/{transactionId}")
  public ResponseEntity<Map<String, Object>> getTransactionDetails(
      @PathVariable UUID accountId,
      @PathVariable String transactionId,
      @RequestParam(required = false) String accept,
      @RequestParam(required = false) String storeId) {
    Map<String, Object> details = accountService.getTransactionDetails(accountId, transactionId, accept, storeId);
    return ResponseEntity.ok(details);
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
