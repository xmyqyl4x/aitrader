package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.AccountDto;
import com.myqyl.aitradex.api.dto.CreateAccountRequest;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.User;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.AccountRepository;
import com.myqyl.aitradex.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

  public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public AccountDto create(CreateAccountRequest request) {
    User user =
        userRepository.findById(request.userId()).orElseThrow(() -> userNotFound(request.userId()));
    Account account =
        Account.builder()
            .user(user)
            .baseCurrency(request.baseCurrency().toUpperCase())
            .cashBalance(request.initialCash().setScale(4, BigDecimal.ROUND_HALF_UP))
            .build();
    return toDto(accountRepository.save(account));
  }

  @Transactional(readOnly = true)
  public List<AccountDto> list() {
    return accountRepository.findAll().stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public AccountDto get(UUID id) {
    return accountRepository.findById(id).map(this::toDto).orElseThrow(() -> accountNotFound(id));
  }

  private AccountDto toDto(Account account) {
    return new AccountDto(account.getId(), account.getUser().getId(), account.getBaseCurrency(),
        account.getCashBalance(), account.getCreatedAt());
  }

  private NotFoundException userNotFound(UUID id) {
    return new NotFoundException("User %s not found".formatted(id));
  }

  private NotFoundException accountNotFound(UUID id) {
    return new NotFoundException("Account %s not found".formatted(id));
  }
}
