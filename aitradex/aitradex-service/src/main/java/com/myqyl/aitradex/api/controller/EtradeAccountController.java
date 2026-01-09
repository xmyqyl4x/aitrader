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
    log.debug("GET /api/etrade/accounts - userId: {}", userId);
    
    try {
      List<EtradeAccountDto> accounts;
      if (userId == null) {
        // If no userId provided, get all accounts (for MVP/demo purposes)
        log.debug("No userId provided, retrieving all accounts from database");
        accounts = accountService.getAllAccounts();
      } else {
        log.debug("Retrieving accounts for userId: {}", userId);
        accounts = accountService.getUserAccounts(userId);
      }
      
      log.debug("Found {} account(s) for userId: {}", accounts.size(), userId);
      return ResponseEntity.ok(accounts);
    } catch (Exception e) {
      log.error("Failed to get user accounts for userId: {}", userId, e);
      throw e;
    }
  }

  /**
   * Gets account details.
   */
  @GetMapping("/{accountId}")
  public ResponseEntity<EtradeAccountDto> getAccount(@PathVariable UUID accountId) {
    log.debug("GET /api/etrade/accounts/{}", accountId);
    
    try {
      EtradeAccountDto account = accountService.getAccount(accountId);
      log.debug("Retrieved account: {} ({})", account.accountName(), account.accountIdKey());
      return ResponseEntity.ok(account);
    } catch (Exception e) {
      log.error("Failed to get account: {}", accountId, e);
      throw e;
    }
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
    log.debug("POST /api/etrade/accounts/sync - userId: {}, accountId: {}", userId, accountId);
    
    try {
      if (userId == null) {
        log.debug("No userId provided, generating temporary userId for sync");
        userId = UUID.randomUUID(); // For MVP
      }
      
      log.debug("Syncing accounts from E*TRADE for accountId: {}", accountId);
      List<EtradeAccountDto> accounts = accountService.syncAccounts(userId, accountId);
      log.debug("Successfully synced {} account(s) for accountId: {}", accounts.size(), accountId);
      return ResponseEntity.ok(accounts);
    } catch (Exception e) {
      log.error("Failed to sync accounts for accountId: {}", accountId, e);
      throw e;
    }
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
