package com.myqyl.aitradex.etrade.service;

import com.myqyl.aitradex.api.dto.EtradeAccountDto;
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
 */
@Service
public class EtradeAccountService {

  private static final Logger log = LoggerFactory.getLogger(EtradeAccountService.class);

  private final EtradeAccountRepository accountRepository;
  private final EtradeAccountClient accountClient;

  public EtradeAccountService(
      EtradeAccountRepository accountRepository,
      EtradeAccountClient accountClient) {
    this.accountRepository = accountRepository;
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
   * Gets account balance from E*TRADE.
   */
  public Map<String, Object> getAccountBalance(UUID accountId, String accountIdKey) {
    return accountClient.getBalance(accountId, accountIdKey);
  }

  /**
   * Gets account portfolio from E*TRADE.
   */
  public Map<String, Object> getAccountPortfolio(UUID accountId, String accountIdKey) {
    return accountClient.getPortfolio(accountId, accountIdKey);
  }

  /**
   * Syncs account list from E*TRADE and updates database.
   */
  @Transactional
  public List<EtradeAccountDto> syncAccounts(UUID userId, UUID accountId) {
    List<Map<String, Object>> accounts = accountClient.getAccountList(accountId);
    
    for (Map<String, Object> accountData : accounts) {
      String accountIdKey = (String) accountData.get("accountIdKey");
      accountRepository.findByAccountIdKey(accountIdKey)
          .ifPresentOrElse(
              account -> {
                account.setAccountType((String) accountData.get("accountType"));
                account.setAccountName((String) accountData.get("accountName"));
                account.setAccountStatus((String) accountData.get("accountStatus"));
                account.setLastSyncedAt(OffsetDateTime.now());
                accountRepository.save(account);
              },
              () -> {
                EtradeAccount account = new EtradeAccount();
                account.setUserId(userId);
                account.setAccountIdKey(accountIdKey);
                account.setAccountType((String) accountData.get("accountType"));
                account.setAccountName((String) accountData.get("accountName"));
                account.setAccountStatus((String) accountData.get("accountStatus"));
                account.setLinkedAt(OffsetDateTime.now());
                account.setLastSyncedAt(OffsetDateTime.now());
                accountRepository.save(account);
              });
    }

    return getUserAccounts(userId);
  }

  /**
   * Gets account transactions from E*TRADE.
   */
  public List<Map<String, Object>> getAccountTransactions(UUID accountId, String marker, Integer count) {
    EtradeAccountDto account = getAccount(accountId);
    return accountClient.getTransactions(accountId, account.accountIdKey(), marker, count);
  }

  /**
   * Gets transaction details from E*TRADE.
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
