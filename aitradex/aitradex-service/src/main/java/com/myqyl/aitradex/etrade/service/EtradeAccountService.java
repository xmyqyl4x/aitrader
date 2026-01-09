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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.accounts.dto.CashBalance;
import com.myqyl.aitradex.etrade.accounts.dto.ComputedBalance;
import com.myqyl.aitradex.etrade.accounts.dto.MarginBalance;
import com.myqyl.aitradex.etrade.accounts.dto.PositionDto;
import com.myqyl.aitradex.etrade.domain.EtradeAccount;
import com.myqyl.aitradex.etrade.domain.EtradeBalance;
import com.myqyl.aitradex.etrade.domain.EtradePortfolioPosition;
import com.myqyl.aitradex.etrade.domain.EtradeTransaction;
import com.myqyl.aitradex.etrade.repository.EtradeAccountRepository;
import com.myqyl.aitradex.etrade.repository.EtradeBalanceRepository;
import com.myqyl.aitradex.etrade.repository.EtradePortfolioPositionRepository;
import com.myqyl.aitradex.etrade.repository.EtradeTransactionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final EtradeBalanceRepository balanceRepository;
  private final EtradeTransactionRepository transactionRepository;
  private final EtradePortfolioPositionRepository positionRepository;
  private final ObjectMapper objectMapper;

  public EtradeAccountService(
      EtradeAccountRepository accountRepository,
      EtradeApiClientAccountAPI accountsApi,
      EtradeAccountClient accountClient,
      EtradeBalanceRepository balanceRepository,
      EtradeTransactionRepository transactionRepository,
      EtradePortfolioPositionRepository positionRepository,
      ObjectMapper objectMapper) {
    this.accountRepository = accountRepository;
    this.accountsApi = accountsApi;
    this.accountClient = accountClient;
    this.balanceRepository = balanceRepository;
    this.transactionRepository = transactionRepository;
    this.positionRepository = positionRepository;
    this.objectMapper = objectMapper;
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
    log.debug("Getting accounts for userId: {}", userId);
    List<EtradeAccountDto> accounts = accountRepository.findByUserId(userId).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
    log.debug("Found {} account(s) for userId: {}", accounts.size(), userId);
    return accounts;
  }

  /**
   * Gets all linked accounts (for MVP/demo when userId is not provided).
   */
  public List<EtradeAccountDto> getAllAccounts() {
    log.debug("Getting all accounts from database");
    List<EtradeAccountDto> accounts = accountRepository.findAll().stream()
        .map(this::toDto)
        .collect(Collectors.toList());
    log.debug("Found {} total account(s) in database", accounts.size());
    return accounts;
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
   * Gets account balance from E*TRADE using the Accounts API and persists as a new snapshot.
   * 
   * Always creates a new balance entry (append-only snapshot/history).
   * Each call generates a new row, even if values are unchanged.
   * 
   * @param accountId Internal account UUID
   * @param accountIdKey E*TRADE account ID key
   * @param instType Institution type (default: "BROKERAGE")
   * @param accountType Account type filter (optional)
   * @param realTimeNAV Whether to get real-time NAV (default: true)
   * @return BalanceResponse DTO
   */
  @Transactional
  public BalanceResponse getAccountBalance(UUID accountId, String accountIdKey, 
                                           String instType, String accountType, Boolean realTimeNAV) {
    BalanceRequest request = new BalanceRequest(instType, accountType, realTimeNAV);
    BalanceResponse balanceResponse = accountsApi.getAccountBalance(accountId, accountIdKey, request);
    
    // Persist balance snapshot (always create new row)
    persistBalanceSnapshot(accountId, balanceResponse);
    
    return balanceResponse;
  }

  /**
   * Gets account balance from E*TRADE (simplified version with defaults) and persists as a new snapshot.
   * 
   * @return BalanceResponse DTO
   */
  @Transactional
  public BalanceResponse getAccountBalance(UUID accountId, String accountIdKey) {
    BalanceRequest request = new BalanceRequest();
    BalanceResponse balanceResponse = accountsApi.getAccountBalance(accountId, accountIdKey, request);
    
    // Persist balance snapshot (always create new row)
    persistBalanceSnapshot(accountId, balanceResponse);
    
    return balanceResponse;
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
   * Gets account portfolio from E*TRADE using the Accounts API and persists positions.
   * 
   * Upserts portfolio positions by positionId.
   * Each position is inserted if it doesn't exist, or updated if it exists.
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
  @Transactional
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
    PortfolioResponse portfolioResponse = accountsApi.viewPortfolio(accountId, accountIdKey, request);
    
    // Persist portfolio positions (upsert by positionId)
    persistPortfolioPositions(accountId, portfolioResponse);
    
    return portfolioResponse;
  }

  /**
   * Gets account portfolio from E*TRADE (simplified version with defaults) and persists positions.
   * 
   * @return PortfolioResponse DTO
   */
  @Transactional
  public PortfolioResponse getAccountPortfolio(UUID accountId, String accountIdKey) {
    PortfolioRequest request = new PortfolioRequest();
    PortfolioResponse portfolioResponse = accountsApi.viewPortfolio(accountId, accountIdKey, request);
    
    // Persist portfolio positions (upsert by positionId)
    persistPortfolioPositions(accountId, portfolioResponse);
    
    return portfolioResponse;
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
    log.debug("Syncing accounts from E*TRADE - userId: {}, accountId: {}", userId, accountId);
    
    try {
      log.debug("Calling E*TRADE List Accounts API for accountId: {}", accountId);
      AccountListResponse response = accountsApi.listAccounts(accountId);
      List<EtradeAccountModel> accounts = response.getAccountList();
      log.debug("Received {} account(s) from E*TRADE", accounts.size());
      
      final int[] updated = {0};
      final int[] created = {0};
      
      for (EtradeAccountModel accountData : accounts) {
        String accountIdKey = accountData.getAccountIdKey();
        log.debug("Processing account - accountIdKey: {}, accountName: {}", 
            accountIdKey, accountData.getAccountName());
        
        accountRepository.findByAccountIdKey(accountIdKey)
            .ifPresentOrElse(
                existingAccount -> {
                  log.debug("Updating existing account - accountIdKey: {}", accountIdKey);
                  existingAccount.setAccountType(accountData.getAccountType());
                  existingAccount.setAccountName(accountData.getAccountName());
                  existingAccount.setAccountStatus(accountData.getAccountStatus());
                  existingAccount.setLastSyncedAt(OffsetDateTime.now());
                  accountRepository.save(existingAccount);
                  updated[0]++;
                },
                () -> {
                  log.debug("Creating new account - accountIdKey: {}", accountIdKey);
                  EtradeAccount newAccount = new EtradeAccount();
                  newAccount.setUserId(userId);
                  newAccount.setAccountIdKey(accountIdKey);
                  newAccount.setAccountType(accountData.getAccountType());
                  newAccount.setAccountName(accountData.getAccountName());
                  newAccount.setAccountStatus(accountData.getAccountStatus());
                  newAccount.setLinkedAt(OffsetDateTime.now());
                  newAccount.setLastSyncedAt(OffsetDateTime.now());
                  accountRepository.save(newAccount);
                  created[0]++;
                });
      }

      log.debug("Account sync completed - created: {}, updated: {}, total: {}", 
          created[0], updated[0], accounts.size());
      
      List<EtradeAccountDto> result = getUserAccounts(userId);
      log.debug("Returning {} account(s) for userId: {}", result.size(), userId);
      return result;
    } catch (Exception e) {
      log.error("Failed to sync accounts for userId: {}, accountId: {}", userId, accountId, e);
      throw e;
    }
  }

  /**
   * Gets account transactions from E*TRADE and persists them.
   * 
   * Upserts transactions by transactionId.
   * Each transaction is inserted if it doesn't exist, or updated if it exists.
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
  @Transactional
  public Map<String, Object> getAccountTransactions(UUID accountId, String marker, Integer count,
                                                     String startDate, String endDate, String sortOrder,
                                                     String accept, String storeId) {
    EtradeAccountDto account = getAccount(accountId);
    Map<String, Object> result = accountClient.getTransactions(accountId, account.accountIdKey(), marker, count,
                                         startDate, endDate, sortOrder, accept, storeId);
    
    // Persist transactions (upsert by transactionId)
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("transactions");
    if (transactions != null) {
      for (Map<String, Object> transactionData : transactions) {
        persistTransaction(accountId, transactionData);
      }
    }
    
    return result;
  }

  /**
   * Gets account transactions from E*TRADE (simplified version) and persists them.
   */
  @Transactional
  public List<Map<String, Object>> getAccountTransactions(UUID accountId, String marker, Integer count) {
    EtradeAccountDto account = getAccount(accountId);
    List<Map<String, Object>> transactions = accountClient.getTransactions(accountId, account.accountIdKey(), marker, count);
    
    // Persist transactions (upsert by transactionId)
    for (Map<String, Object> transactionData : transactions) {
      persistTransaction(accountId, transactionData);
    }
    
    return transactions;
  }

  /**
   * Gets transaction details from E*TRADE and persists/updates the transaction details.
   * 
   * Upserts transaction details by transactionId.
   * If transaction exists, updates it with details. If not, creates a new transaction record.
   * 
   * @param accountId Internal account UUID
   * @param transactionId Transaction ID
   * @param accept Response format ("xml" or "json") (optional)
   * @param storeId Store ID filter (optional)
   */
  @Transactional
  public Map<String, Object> getTransactionDetails(UUID accountId, String transactionId,
                                                    String accept, String storeId) {
    EtradeAccountDto account = getAccount(accountId);
    Map<String, Object> details = accountClient.getTransactionDetails(accountId, account.accountIdKey(), transactionId, accept, storeId);
    
    // Persist/update transaction details (upsert by transactionId)
    persistTransactionDetails(accountId, transactionId, details);
    
    return details;
  }

  /**
   * Gets transaction details from E*TRADE (simplified version) and persists/updates the transaction details.
   */
  @Transactional
  public Map<String, Object> getTransactionDetails(UUID accountId, String transactionId) {
    EtradeAccountDto account = getAccount(accountId);
    Map<String, Object> details = accountClient.getTransactionDetails(accountId, account.accountIdKey(), transactionId);
    
    // Persist/update transaction details (upsert by transactionId)
    persistTransactionDetails(accountId, transactionId, details);
    
    return details;
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

  /**
   * Persists a balance snapshot (always creates a new row - append-only history).
   */
  private void persistBalanceSnapshot(UUID accountId, BalanceResponse balanceResponse) {
    try {
      EtradeBalance balance = new EtradeBalance();
      balance.setAccountId(accountId);
      balance.setSnapshotTime(OffsetDateTime.now());
      
      // Account metadata
      balance.setAccountIdFromResponse(balanceResponse.getAccountId());
      balance.setAccountType(balanceResponse.getAccountType());
      balance.setAccountDescription(balanceResponse.getAccountDescription());
      balance.setAccountMode(balanceResponse.getAccountMode());
      
      // Cash section
      CashBalance cash = balanceResponse.getCash();
      if (cash != null) {
        balance.setCashBalance(toBigDecimal(cash.getCashBalance()));
        balance.setCashAvailable(toBigDecimal(cash.getCashAvailable()));
        balance.setUnclearedDeposits(toBigDecimal(cash.getUnclearedDeposits()));
        balance.setCashSweep(toBigDecimal(cash.getCashSweep()));
      }
      
      // Margin section
      MarginBalance margin = balanceResponse.getMargin();
      if (margin != null) {
        balance.setMarginBalance(toBigDecimal(margin.getMarginBalance()));
        balance.setMarginAvailable(toBigDecimal(margin.getMarginAvailable()));
        balance.setMarginBuyingPower(toBigDecimal(margin.getMarginBuyingPower()));
        balance.setDayTradingBuyingPower(toBigDecimal(margin.getDayTradingBuyingPower()));
      }
      
      // Computed section
      ComputedBalance computed = balanceResponse.getComputed();
      if (computed != null) {
        balance.setTotalValue(toBigDecimal(computed.getTotalValue()));
        balance.setNetValue(toBigDecimal(computed.getNetValue()));
        balance.setSettledCash(toBigDecimal(computed.getSettledCash()));
        balance.setOpenCalls(toBigDecimal(computed.getOpenCalls()));
        balance.setOpenPuts(toBigDecimal(computed.getOpenPuts()));
      }
      
      // Optional: Store raw response as JSON
      try {
        balance.setRawResponse(objectMapper.writeValueAsString(balanceResponse));
      } catch (Exception e) {
        log.warn("Failed to serialize balance response to JSON", e);
      }
      
      balanceRepository.save(balance);
      log.debug("Persisted balance snapshot for account {}", accountId);
    } catch (Exception e) {
      log.error("Failed to persist balance snapshot for account {}", accountId, e);
      // Don't throw - persistence failure should not break the API call
    }
  }

  /**
   * Persists/updates a transaction (upsert by transactionId).
   */
  private void persistTransaction(UUID accountId, Map<String, Object> transactionData) {
    try {
      String transactionId = (String) transactionData.get("transactionId");
      if (transactionId == null || transactionId.isEmpty()) {
        log.warn("Transaction data missing transactionId, skipping persistence");
        return;
      }
      
      Optional<EtradeTransaction> existing = transactionRepository.findByTransactionId(transactionId);
      EtradeTransaction transaction;
      
      if (existing.isPresent()) {
        // Update existing transaction
        transaction = existing.get();
        transaction.setLastUpdatedAt(OffsetDateTime.now());
      } else {
        // Create new transaction
        transaction = new EtradeTransaction();
        transaction.setAccountId(accountId);
        transaction.setTransactionId(transactionId);
        transaction.setFirstSeenAt(OffsetDateTime.now());
        transaction.setLastUpdatedAt(OffsetDateTime.now());
      }
      
      // Update fields from transaction data
      transaction.setAccountIdFromResponse((String) transactionData.get("accountId"));
      Object transactionDateObj = transactionData.get("transactionDate");
      if (transactionDateObj != null) {
        if (transactionDateObj instanceof Long) {
          transaction.setTransactionDate((Long) transactionDateObj);
        } else if (transactionDateObj instanceof String) {
          try {
            transaction.setTransactionDate(Long.parseLong((String) transactionDateObj));
          } catch (NumberFormatException e) {
            log.warn("Invalid transaction date format: {}", transactionDateObj);
          }
        }
      }
      transaction.setAmount(toBigDecimal(transactionData.get("amount")));
      transaction.setDescription((String) transactionData.get("description"));
      transaction.setTransactionType((String) transactionData.get("transactionType"));
      transaction.setInstType((String) transactionData.get("instType"));
      transaction.setDetailsUri((String) transactionData.get("detailsURI"));
      
      // Optional: Store raw response as JSON
      try {
        transaction.setRawResponse(objectMapper.writeValueAsString(transactionData));
      } catch (Exception e) {
        log.warn("Failed to serialize transaction data to JSON", e);
      }
      
      transactionRepository.save(transaction);
      log.debug("Persisted transaction {} for account {}", transactionId, accountId);
    } catch (Exception e) {
      log.error("Failed to persist transaction for account {}", accountId, e);
      // Don't throw - persistence failure should not break the API call
    }
  }

  /**
   * Persists/updates transaction details (upsert by transactionId).
   */
  private void persistTransactionDetails(UUID accountId, String transactionId, Map<String, Object> details) {
    try {
      Optional<EtradeTransaction> existing = transactionRepository.findByTransactionId(transactionId);
      EtradeTransaction transaction;
      
      if (existing.isPresent()) {
        // Update existing transaction with details
        transaction = existing.get();
        transaction.setLastUpdatedAt(OffsetDateTime.now());
      } else {
        // Create new transaction (shouldn't happen, but handle gracefully)
        transaction = new EtradeTransaction();
        transaction.setAccountId(accountId);
        transaction.setTransactionId(transactionId);
        transaction.setFirstSeenAt(OffsetDateTime.now());
        transaction.setLastUpdatedAt(OffsetDateTime.now());
      }
      
      // Update basic fields if not already set
      if (transaction.getAccountIdFromResponse() == null) {
        transaction.setAccountIdFromResponse((String) details.get("accountId"));
      }
      if (transaction.getTransactionDate() == null) {
        Object transactionDateObj = details.get("transactionDate");
        if (transactionDateObj != null) {
          if (transactionDateObj instanceof Long) {
            transaction.setTransactionDate((Long) transactionDateObj);
          } else if (transactionDateObj instanceof String) {
            try {
              transaction.setTransactionDate(Long.parseLong((String) transactionDateObj));
            } catch (NumberFormatException e) {
              log.warn("Invalid transaction date format: {}", transactionDateObj);
            }
          }
        }
      }
      if (transaction.getAmount() == null) {
        transaction.setAmount(toBigDecimal(details.get("amount")));
      }
      if (transaction.getDescription() == null) {
        transaction.setDescription((String) details.get("description"));
      }
      
      // Update detail-specific fields
      @SuppressWarnings("unchecked")
      Map<String, Object> category = (Map<String, Object>) details.get("category");
      if (category != null) {
        transaction.setCategoryId((String) category.get("categoryId"));
        transaction.setCategoryParentId((String) category.get("parentId"));
      }
      
      @SuppressWarnings("unchecked")
      Map<String, Object> brokerage = (Map<String, Object>) details.get("brokerage");
      if (brokerage != null) {
        transaction.setBrokerageTransactionType((String) brokerage.get("transactionType"));
      }
      
      // Store details raw response
      try {
        transaction.setDetailsRawResponse(objectMapper.writeValueAsString(details));
      } catch (Exception e) {
        log.warn("Failed to serialize transaction details to JSON", e);
      }
      
      transactionRepository.save(transaction);
      log.debug("Persisted transaction details for transaction {} in account {}", transactionId, accountId);
    } catch (Exception e) {
      log.error("Failed to persist transaction details for transaction {} in account {}", transactionId, accountId, e);
      // Don't throw - persistence failure should not break the API call
    }
  }

  /**
   * Persists/updates portfolio positions (upsert by positionId).
   */
  private void persistPortfolioPositions(UUID accountId, PortfolioResponse portfolioResponse) {
    try {
      List<PositionDto> positions = portfolioResponse.getAllPositions();
      if (positions == null || positions.isEmpty()) {
        log.debug("No positions to persist for account {}", accountId);
        return;
      }
      
      OffsetDateTime snapshotTime = OffsetDateTime.now();
      
      for (PositionDto positionDto : positions) {
        if (positionDto.getPositionId() == null) {
          log.warn("Position missing positionId, skipping persistence");
          continue;
        }
        
        Optional<EtradePortfolioPosition> existing = positionRepository.findByAccountIdAndPositionId(
            accountId, positionDto.getPositionId());
        EtradePortfolioPosition position;
        
        if (existing.isPresent()) {
          // Update existing position
          position = existing.get();
          position.setLastUpdatedAt(OffsetDateTime.now());
          position.setSnapshotTime(snapshotTime);
        } else {
          // Create new position
          position = new EtradePortfolioPosition();
          position.setAccountId(accountId);
          position.setPositionId(positionDto.getPositionId());
          position.setFirstSeenAt(OffsetDateTime.now());
          position.setLastUpdatedAt(OffsetDateTime.now());
          position.setSnapshotTime(snapshotTime);
        }
        
        // Update all position fields from DTO
        updatePositionFromDto(position, positionDto);
        
        // Optional: Store raw response as JSON
        try {
          position.setRawResponse(objectMapper.writeValueAsString(positionDto));
        } catch (Exception e) {
          log.warn("Failed to serialize position to JSON", e);
        }
        
        positionRepository.save(position);
        log.debug("Persisted position {} for account {}", positionDto.getPositionId(), accountId);
      }
      
      log.info("Persisted {} positions for account {}", positions.size(), accountId);
    } catch (Exception e) {
      log.error("Failed to persist portfolio positions for account {}", accountId, e);
      // Don't throw - persistence failure should not break the API call
    }
  }

  /**
   * Updates position entity from PositionDto.
   */
  private void updatePositionFromDto(EtradePortfolioPosition position, PositionDto dto) {
    // Product information
    if (dto.getProduct() != null) {
      position.setSymbol(dto.getProduct().getSymbol());
      position.setSecurityType(dto.getProduct().getSecurityType());
    }
    // PositionDto has cusip, exchange, isQuotable directly, not in ProductDto
    position.setCusip(dto.getCusip());
    position.setExchange(dto.getExchange());
    position.setIsQuotable(dto.getIsQuotable());
    position.setSymbolDescription(dto.getSymbolDescription());
    
    // Position details
    position.setDateAcquired(dto.getDateAcquired());
    position.setPricePaid(toBigDecimal(dto.getPricePaid()));
    position.setCommissions(toBigDecimal(dto.getCommissions()));
    position.setOtherFees(toBigDecimal(dto.getOtherFees()));
    position.setQuantity(toBigDecimal(dto.getQuantity()));
    position.setPositionIndicator(dto.getPositionIndicator());
    position.setPositionType(dto.getPositionType());
    
    // Market values
    position.setDaysGain(toBigDecimal(dto.getDaysGain()));
    position.setDaysGainPct(toBigDecimal(dto.getDaysGainPct()));
    position.setMarketValue(toBigDecimal(dto.getMarketValue()));
    position.setTotalCost(toBigDecimal(dto.getTotalCost()));
    position.setTotalGain(toBigDecimal(dto.getTotalGain()));
    position.setTotalGainPct(toBigDecimal(dto.getTotalGainPct()));
    position.setPctOfPortfolio(toBigDecimal(dto.getPctOfPortfolio()));
    position.setCostPerShare(toBigDecimal(dto.getCostPerShare()));
    position.setGainLoss(toBigDecimal(dto.getGainLoss()));
    position.setGainLossPercent(toBigDecimal(dto.getGainLossPercent()));
    position.setCostBasis(toBigDecimal(dto.getCostBasis()));
    
    // Option-specific fields
    position.setIntrinsicValue(toBigDecimal(dto.getIntrinsicValue()));
    position.setTimeValue(toBigDecimal(dto.getTimeValue()));
    position.setMultiplier(dto.getMultiplier());
    position.setDigits(dto.getDigits());
    
    // URLs
    position.setLotsDetailsUri(dto.getLotsDetails());
    position.setQuoteDetailsUri(dto.getQuoteDetails());
  }

  /**
   * Helper method to convert Object to BigDecimal safely.
   */
  private BigDecimal toBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal) {
      return (BigDecimal) value;
    }
    if (value instanceof Double) {
      return BigDecimal.valueOf((Double) value);
    }
    if (value instanceof Long) {
      return BigDecimal.valueOf((Long) value);
    }
    if (value instanceof Integer) {
      return BigDecimal.valueOf((Integer) value);
    }
    if (value instanceof String) {
      try {
        return new BigDecimal((String) value);
      } catch (NumberFormatException e) {
        log.warn("Invalid number format: {}", value);
        return null;
      }
    }
    log.warn("Cannot convert {} to BigDecimal", value.getClass().getName());
    return null;
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
