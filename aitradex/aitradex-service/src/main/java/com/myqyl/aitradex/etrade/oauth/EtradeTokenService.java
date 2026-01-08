package com.myqyl.aitradex.etrade.oauth;

import com.myqyl.aitradex.etrade.domain.EtradeOAuthToken;
import com.myqyl.aitradex.etrade.repository.EtradeOAuthTokenRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing OAuth token storage and retrieval.
 */
@Service
public class EtradeTokenService {

  private static final Logger log = LoggerFactory.getLogger(EtradeTokenService.class);

  private final EtradeOAuthTokenRepository tokenRepository;
  private final EtradeTokenEncryption tokenEncryption;

  public EtradeTokenService(
      EtradeOAuthTokenRepository tokenRepository,
      EtradeTokenEncryption tokenEncryption) {
    this.tokenRepository = tokenRepository;
    this.tokenEncryption = tokenEncryption;
  }

  /**
   * Stores access token for an account.
   */
  @Transactional
  public void storeAccessToken(UUID accountId, String accessToken, String accessTokenSecret) {
    Optional<EtradeOAuthToken> existing = tokenRepository.findByAccountId(accountId);
    
    EtradeOAuthToken token;
    if (existing.isPresent()) {
      token = existing.get();
    } else {
      token = new EtradeOAuthToken();
      token.setAccountId(accountId);
    }
    
    token.setAccessTokenEncrypted(tokenEncryption.encrypt(accessToken));
    token.setAccessTokenSecretEncrypted(tokenEncryption.encrypt(accessTokenSecret));
    
    tokenRepository.save(token);
    log.info("Access token stored for account {}", accountId);
  }

  /**
   * Gets access token pair for an account (decrypted).
   */
  public Optional<AccessTokenPair> getAccessToken(UUID accountId) {
    return tokenRepository.findByAccountId(accountId)
        .map(token -> new AccessTokenPair(
            tokenEncryption.decrypt(token.getAccessTokenEncrypted()),
            tokenEncryption.decrypt(token.getAccessTokenSecretEncrypted())));
  }

  /**
   * Deletes access token for an account.
   */
  @Transactional
  public void deleteAccessToken(UUID accountId) {
    tokenRepository.deleteByAccountId(accountId);
    log.info("Access token deleted for account {}", accountId);
  }

  /**
   * Checks if account has valid access token.
   */
  public boolean hasValidToken(UUID accountId) {
    return tokenRepository.findByAccountId(accountId).isPresent();
  }

  /**
   * Access token pair.
   */
  public static class AccessTokenPair {
    private final String accessToken;
    private final String accessTokenSecret;

    public AccessTokenPair(String accessToken, String accessTokenSecret) {
      this.accessToken = accessToken;
      this.accessTokenSecret = accessTokenSecret;
    }

    public String getAccessToken() {
      return accessToken;
    }

    public String getAccessTokenSecret() {
      return accessTokenSecret;
    }
  }
}
