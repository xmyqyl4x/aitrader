package com.myqyl.aitradex.etrade.service;

import com.myqyl.aitradex.api.dto.EtradeAccountDto;
import com.myqyl.aitradex.etrade.accounts.dto.AccountListResponse;
import com.myqyl.aitradex.etrade.accounts.dto.BalanceRequest;
import com.myqyl.aitradex.etrade.accounts.dto.BalanceResponse;
import com.myqyl.aitradex.etrade.accounts.dto.EtradeAccountModel;
import com.myqyl.aitradex.etrade.accounts.dto.PortfolioRequest;
import com.myqyl.aitradex.etrade.accounts.dto.PortfolioResponse;
import com.myqyl.aitradex.etrade.client.EtradeApiClientAccountAPI;
import com.myqyl.aitradex.etrade.client.EtradeAccountClient;
import com.myqyl.aitradex.etrade.domain.EtradeAccount;
import com.myqyl.aitradex.etrade.repository.EtradeAccountRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing E*TRADE accounts.
 * 
 * Delegates to EtradeApiClientAccountAPI for Accounts API calls using DTOs.
 * Maintains backward compatibility with Map-based methods through the legacy EtradeAccountClient.
 */
@Service
public class EtradeAccountService {

  private static final Logger log = LoggerFactory.getLogger(EtradeAccountService.class);

  private final EtradeAccountRepository accountRepository;
  private final EtradeApiClientAccountAPI accountsApi;
  private final EtradeAccountClient accountClient; // Legacy client for backward compatibility

  public EtradeAccountService(
      EtradeAccountRepository accountRepository,
      EtradeApiClientAccountAPI accountsApi,
      EtradeAccountClient accountClient) {
    this.accountRepository = accountRepository;
    this.accountsApi = accountsApi;
    this.accountClient = accountClient;
  }

  /**
   * Links an E*TRADE account for a user.
   */
  @Transactional
  public EtradeAccountDto linkAccount(UUID userId, String accountIdKey, Map<String, Object> accountData) {
    // Check if account already linked
    accountRepository.findByAccountIdKey(accountIdKey)
        .ifPresent(account -> {
          throw new RuntimeException("Account already linked");
        });

    EtradeAccount account = new EtradeAccount();
    account.setUserId(userId);
    account.setAccountIdKey(accountIdKey);
    account.setAccountType((String) accountData.get("accountType"));
    account.setAccountName((String) accountData.get("accountName"));
    account.setAccountStatus((String) accountData.get("accountStatus"));
    account.setLinkedAt(OffsetDateTime.now());

    EtradeAccount saved = accountRepository.save(account);
    log.info("Linked E*TRADE account {} for user {}", accountIdKey, userId);

    return toDto(saved);
  }

  /**
   * Gets all linked accounts for a user.
   */
  public List<EtradeAccountDto> getUserAccounts(UUID userId) {
    return accountRepository.findByUserId(userId).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  /**
   * Gets account by ID.
   */
  public EtradeAccountDto getAccount(UUID accountId) {
    return accountRepository.findById(accountId)
        .map(this::toDto)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
  }

  /**
   * Gets account balance from E*TRADE using the Accounts API.
   * 
   * @param accountId Internal account UUID
   * @param accountIdKey E*TRADE account ID key
   * @param instType Institution type (default: "BROKERAGE")
   * @param accountType Account type filter (optional)
   * @param realTimeNAV Whether to get real-time NAV (default: true)
   * @return BalanceResponse DTO
   */
  public BalanceResponse getAccountBalance(UUID accountId, String accountIdKey, 
                                           String instType, String accountType, Boolean realTimeNAV) {
    BalanceRequest request = new BalanceRequest(instType, accountType, realTimeNAV);
    return accountsApi.getAccountBalance(accountId, accountIdKey, request);
  }

  /**
   * Gets account balance from E*TRADE (simplified version with defaults).
   * 
   * @return BalanceResponse DTO
   */
  public BalanceResponse getAccountBalance(UUID accountId, String accountIdKey) {
    BalanceRequest request = new BalanceRequest();
    return accountsApi.getAccountBalance(accountId, accountIdKey, request);
  }

  /**
   * Gets account balance as Map (legacy method for backward compatibility).
   * 
   * @deprecated Use getAccountBalance() returning BalanceResponse instead
   */
  @Deprecated
  public Map<String, Object> getAccountBalanceAsMap(UUID accountId, String accountIdKey, 
                                                      String instType, String accountType, Boolean realTimeNAV) {
    return accountClient.getBalance(accountId, accountIdKey, instType, accountType, realTimeNAV);
  }

  /**
   * Gets account portfolio from E*TRADE using the Accounts API.
   * 
   * @param accountId Internal account UUID
   * @param accountIdKey E*TRADE account ID key
   * @param count Number of positions to return (optional)
   * @param sortBy Sort field (e.g., "SYMBOL", "QUANTITY", "MARKET_VALUE") (optional)
   * @param sortOrder Sort direction ("ASC", "DESC") (optional)
   * @param pageNumber Page number for pagination (optional)
   * @param marketSession Market session filter (optional)
   * @param totalsRequired Whether to include totals (optional)
   * @param lotsRequired Whether to include lot details (optional)
   * @param view View type (e.g., "QUICK", "COMPLETE") (optional)
   * @return PortfolioResponse DTO
   */
  public PortfolioResponse getAccountPortfolio(UUID accountId, String accountIdKey, Integer count,
                                               String sortBy, String sortOrder, Integer pageNumber,
                                               String marketSession, Boolean totalsRequired,
                                               Boolean lotsRequired, String view) {
    PortfolioRequest request = new PortfolioRequest();
    request.setCount(count);
    request.setSortBy(sortBy);
    request.setSortOrder(sortOrder);
    request.setPageNumber(pageNumber);
    request.setMarketSession(marketSession);
    request.setTotalsRequired(totalsRequired);
    request.setLotsRequired(lotsRequired);
    request.setView(view);
    return accountsApi.viewPortfolio(accountId, accountIdKey, request);
  }

  /**
   * Gets account portfolio from E*TRADE (simplified version with defaults).
   * 
   * @return PortfolioResponse DTO
   */
  public PortfolioResponse getAccountPortfolio(UUID accountId, String accountIdKey) {
    PortfolioRequest request = new PortfolioRequest();
    return accountsApi.viewPortfolio(accountId, accountIdKey, request);
  }

  /**
   * Gets account portfolio as Map (legacy method for backward compatibility).
   * 
   * @deprecated Use getAccountPortfolio() returning PortfolioResponse instead
   */
  @Deprecated
  public Map<String, Object> getAccountPortfolioAsMap(UUID accountId, String accountIdKey, Integer count,
                                                       String sortBy, String sortOrder, Integer pageNumber,
                                                       String marketSession, Boolean totalsRequired,
                                                       Boolean lotsRequired, String view) {
    return accountClient.getPortfolio(accountId, accountIdKey, count, sortBy, sortOrder, pageNumber,
                                      marketSession, totalsRequired, lotsRequired, view);
  }

  /**
   * Syncs account list from E*TRADE and updates database using the Accounts API.
   */
  @Transactional
  public List<EtradeAccountDto> syncAccounts(UUID userId, UUID accountId) {
    AccountListResponse response = accountsApi.listAccounts(accountId);
    List<EtradeAccountModel> accounts = response.getAccountList();
    
    for (EtradeAccountModel accountData : accounts) {
      String accountIdKey = accountData.getAccountIdKey();
      accountRepository.findByAccountIdKey(accountIdKey)
          .ifPresentOrElse(
              account -> {
                account.setAccountType(accountData.getAccountType());
                account.setAccountName(accountData.getAccountName());
                account.setAccountStatus(accountData.getAccountStatus());
                account.setLastSyncedAt(OffsetDateTime.now());
                accountRepository.save(account);
              },
              () -> {
                EtradeAccount account = new EtradeAccount();
                account.setUserId(userId);
                account.setAccountIdKey(accountIdKey);
                account.setAccountType(accountData.getAccountType());
                account.setAccountName(accountData.getAccountName());
                account.setAccountStatus(accountData.getAccountStatus());
                account.setLinkedAt(OffsetDateTime.now());
                account.setLastSyncedAt(OffsetDateTime.now());
                accountRepository.save(account);
              });
    }

    return getUserAccounts(userId);
  }

  /**
   * Gets account transactions from E*TRADE.
   * 
   * @param accountId Internal account UUID
   * @param marker Pagination marker (optional)
   * @param count Number of transactions to return (optional)
   * @param startDate Start date filter (MMddyyyy format) (optional)
   * @param endDate End date filter (MMddyyyy format) (optional)
   * @param sortOrder Sort direction ("ASC", "DESC") (optional)
   * @param accept Response format ("xml" or "json") (optional)
   * @param storeId Store ID filter (optional)
   */
  public Map<String, Object> getAccountTransactions(UUID accountId, String marker, Integer count,
                                                     String startDate, String endDate, String sortOrder,
                                                     String accept, String storeId) {
    EtradeAccountDto account = getAccount(accountId);
    return accountClient.getTransactions(accountId, account.accountIdKey(), marker, count,
                                         startDate, endDate, sortOrder, accept, storeId);
  }

  /**
   * Gets account transactions from E*TRADE (simplified version).
   */
  public List<Map<String, Object>> getAccountTransactions(UUID accountId, String marker, Integer count) {
    EtradeAccountDto account = getAccount(accountId);
    return accountClient.getTransactions(accountId, account.accountIdKey(), marker, count);
  }

  /**
   * Gets transaction details from E*TRADE.
   * 
   * @param accountId Internal account UUID
   * @param transactionId Transaction ID
   * @param accept Response format ("xml" or "json") (optional)
   * @param storeId Store ID filter (optional)
   */
  public Map<String, Object> getTransactionDetails(UUID accountId, String transactionId,
                                                    String accept, String storeId) {
    EtradeAccountDto account = getAccount(accountId);
    return accountClient.getTransactionDetails(accountId, account.accountIdKey(), transactionId, accept, storeId);
  }

  /**
   * Gets transaction details from E*TRADE (simplified version).
   */
  public Map<String, Object> getTransactionDetails(UUID accountId, String transactionId) {
    EtradeAccountDto account = getAccount(accountId);
    return accountClient.getTransactionDetails(accountId, account.accountIdKey(), transactionId);
  }

  /**
   * Unlinks an account.
   */
  @Transactional
  public void unlinkAccount(UUID accountId) {
    accountRepository.findById(accountId)
        .ifPresentOrElse(
            account -> {
              accountRepository.delete(account);
              log.info("Unlinked E*TRADE account {}", account.getAccountIdKey());
            },
            () -> {
              throw new RuntimeException("Account not found: " + accountId);
            });
  }

  private EtradeAccountDto toDto(EtradeAccount account) {
    return new EtradeAccountDto(
        account.getId(),
        account.getUserId(),
        account.getAccountIdKey(),
        account.getAccountType(),
        account.getAccountName(),
        account.getAccountStatus(),
        account.getLinkedAt(),
        account.getLastSyncedAt());
  }
}
