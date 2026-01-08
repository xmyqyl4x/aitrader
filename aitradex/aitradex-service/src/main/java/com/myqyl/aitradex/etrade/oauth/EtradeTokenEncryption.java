package com.myqyl.aitradex.etrade.oauth;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for encrypting and decrypting OAuth tokens.
 * Uses AES-256 encryption for secure token storage.
 */
public class EtradeTokenEncryption {

  private static final Logger log = LoggerFactory.getLogger(EtradeTokenEncryption.class);
  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
  
  private final String encryptionKey;

  public EtradeTokenEncryption(String encryptionKey) {
    if (encryptionKey == null || encryptionKey.length() < 16) {
      throw new IllegalArgumentException("Encryption key must be at least 16 characters");
    }
    // Pad or truncate to 32 bytes (256 bits) for AES-256
    this.encryptionKey = padKey(encryptionKey);
  }

  /**
   * Encrypts a plaintext token.
   */
  public String encrypt(String plaintext) {
    if (plaintext == null || plaintext.isEmpty()) {
      return plaintext;
    }
    
    try {
      SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      log.error("Failed to encrypt token", e);
      throw new RuntimeException("Token encryption failed", e);
    }
  }

  /**
   * Decrypts an encrypted token.
   */
  public String decrypt(String encrypted) {
    if (encrypted == null || encrypted.isEmpty()) {
      return encrypted;
    }
    
    try {
      SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Failed to decrypt token", e);
      throw new RuntimeException("Token decryption failed", e);
    }
  }

  /**
   * Generates a secure encryption key (for initial setup).
   */
  public static String generateKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
      keyGenerator.init(256);
      SecretKey secretKey = keyGenerator.generateKey();
      return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate encryption key", e);
    }
  }

  private String padKey(String key) {
    if (key.length() >= 32) {
      return key.substring(0, 32);
    }
    // Pad to 32 characters
    StringBuilder padded = new StringBuilder(key);
    while (padded.length() < 32) {
      padded.append('0');
    }
    return padded.toString();
  }
}
