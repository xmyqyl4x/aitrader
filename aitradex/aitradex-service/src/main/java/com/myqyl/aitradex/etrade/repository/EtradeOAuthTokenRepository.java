package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EtradeOAuthTokenRepository extends JpaRepository<EtradeOAuthToken, UUID> {
  
  Optional<EtradeOAuthToken> findByAccountId(UUID accountId);
  
  Optional<EtradeOAuthToken> findByRequestToken(String requestToken);
  
  Optional<EtradeOAuthToken> findByCorrelationId(String correlationId);
  
  void deleteByAccountId(UUID accountId);
}
