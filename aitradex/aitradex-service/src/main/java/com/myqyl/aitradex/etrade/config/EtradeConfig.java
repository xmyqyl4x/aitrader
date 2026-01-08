package com.myqyl.aitradex.etrade.config;

import com.myqyl.aitradex.etrade.oauth.EtradeOAuth1Template;
import com.myqyl.aitradex.etrade.oauth.EtradeTokenEncryption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for E*TRADE integration.
 */
@Configuration
@ConditionalOnProperty(name = "app.etrade.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(EtradeProperties.class)
public class EtradeConfig {

  private static final Logger log = LoggerFactory.getLogger(EtradeConfig.class);

  @Bean
  public EtradeOAuth1Template etradeOAuth1Template(EtradeProperties properties) {
    return new EtradeOAuth1Template(properties.getConsumerKey(), properties.getConsumerSecret());
  }

  @Bean
  public EtradeTokenEncryption etradeTokenEncryption(EtradeProperties properties) {
    String encryptionKey = properties.getEncryptionKey();
    if (encryptionKey == null || encryptionKey.isEmpty() || encryptionKey.startsWith("default-")) {
      // Generate a default key for development (NOT FOR PRODUCTION)
      // In production, this should be provided via environment variable
      log.warn("Using generated encryption key for development. Set ETRADE_ENCRYPTION_KEY in production!");
      encryptionKey = EtradeTokenEncryption.generateKey();
    }
    return new EtradeTokenEncryption(encryptionKey);
  }
}
