package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EtradeAccountRepository extends JpaRepository<EtradeAccount, UUID> {
  
  List<EtradeAccount> findByUserId(UUID userId);
  
  Optional<EtradeAccount> findByAccountIdKey(String accountIdKey);
  
  boolean existsByAccountIdKey(String accountIdKey);
}
