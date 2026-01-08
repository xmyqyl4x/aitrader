package com.myqyl.aitradex.etrade.oauth;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * OAuth 1.0 implementation for E*TRADE API.
 * Handles OAuth signing and request token/access token exchange.
 */
@Component
public class EtradeOAuth1Template {

  private static final Logger log = LoggerFactory.getLogger(EtradeOAuth1Template.class);
  private static final String OAUTH_VERSION = "1.0";
  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1"; // Java Mac algorithm name
  private static final String SIGNATURE_METHOD = "HMAC-SHA1"; // OAuth signature method name
  private static final SecureRandom random = new SecureRandom();

  private final String consumerKey;
  private final String consumerSecret;

  public EtradeOAuth1Template(String consumerKey, String consumerSecret) {
    this.consumerKey = consumerKey;
    this.consumerSecret = consumerSecret;
  }

  /**
   * Generates OAuth 1.0 authorization header for a request.
   */
  public String generateAuthorizationHeader(String method, String url, Map<String, String> parameters, 
                                           String token, String tokenSecret) {
    Map<String, String> oauthParams = new TreeMap<>();
    oauthParams.put("oauth_consumer_key", consumerKey);
    oauthParams.put("oauth_nonce", generateNonce());
    oauthParams.put("oauth_signature_method", SIGNATURE_METHOD);
    oauthParams.put("oauth_timestamp", String.valueOf(System.currentTimeMillis() / 1000));
    oauthParams.put("oauth_version", OAUTH_VERSION);
    
    if (token != null) {
      oauthParams.put("oauth_token", token);
    }

    // Merge all parameters
    Map<String, String> allParams = new TreeMap<>(oauthParams);
    if (parameters != null) {
      allParams.putAll(parameters);
    }

    // Generate signature
    String signature = generateSignature(method, url, allParams, tokenSecret);
    oauthParams.put("oauth_signature", signature);

    // Build authorization header
    return "OAuth " + oauthParams.entrySet().stream()
        .map(e -> percentEncode(e.getKey()) + "=\"" + percentEncode(e.getValue()) + "\"")
        .collect(Collectors.joining(", "));
  }

  /**
   * Generates OAuth signature using HMAC-SHA1.
   */
  private String generateSignature(String method, String url, Map<String, String> parameters, 
                                   String tokenSecret) {
    try {
      // Build signature base string
      String baseString = method.toUpperCase() + "&" + 
                         percentEncode(normalizeUrl(url)) + "&" +
                         percentEncode(normalizeParameters(parameters));

      // Build signing key
      String signingKey = percentEncode(consumerSecret) + "&";
      if (tokenSecret != null) {
        signingKey += percentEncode(tokenSecret);
      }

      // Generate HMAC-SHA1 signature
      SecretKeySpec keySpec = new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM);
      Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
      mac.init(keySpec);
      byte[] signature = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
      
      return Base64.getEncoder().encodeToString(signature);
    } catch (Exception e) {
      log.error("Failed to generate OAuth signature", e);
      throw new RuntimeException("OAuth signature generation failed", e);
    }
  }

  /**
   * Generates a random nonce.
   */
  private String generateNonce() {
    byte[] nonceBytes = new byte[16];
    random.nextBytes(nonceBytes);
    return Base64.getEncoder().encodeToString(nonceBytes).replaceAll("[^a-zA-Z0-9]", "");
  }

  /**
   * Normalizes URL for signature base string.
   */
  private String normalizeUrl(String url) {
    try {
      URI uri = URI.create(url);
      String scheme = uri.getScheme().toLowerCase();
      String host = uri.getHost().toLowerCase();
      int port = uri.getPort();
      String path = uri.getPath();
      
      if (port == -1 || (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
        return scheme + "://" + host + (path != null ? path : "");
      }
      return scheme + "://" + host + ":" + port + (path != null ? path : "");
    } catch (Exception e) {
      return url;
    }
  }

  /**
   * Normalizes parameters for signature base string.
   */
  private String normalizeParameters(Map<String, String> parameters) {
    return parameters.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(e -> percentEncode(e.getKey()) + "=" + percentEncode(e.getValue()))
        .collect(Collectors.joining("&"));
  }

  /**
   * URL encodes a string according to OAuth 1.0 spec.
   */
  private String percentEncode(String value) {
    if (value == null) {
      return "";
    }
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
          .replace("+", "%20")
          .replace("*", "%2A")
          .replace("%7E", "~");
    } catch (Exception e) {
      return value;
    }
  }

  /**
   * Parses OAuth response (token and secret).
   */
  public Map<String, String> parseOAuthResponse(String responseBody) {
    Map<String, String> params = new HashMap<>();
    if (responseBody != null && !responseBody.isEmpty()) {
      String[] pairs = responseBody.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=", 2);
        if (keyValue.length == 2) {
          params.put(keyValue[0], decode(keyValue[1]));
        }
      }
    }
    return params;
  }

  private String decode(String value) {
    try {
      return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8.name());
    } catch (Exception e) {
      return value;
    }
  }

  public String getConsumerKey() {
    return consumerKey;
  }

  public String getConsumerSecret() {
    return consumerSecret;
  }
}
