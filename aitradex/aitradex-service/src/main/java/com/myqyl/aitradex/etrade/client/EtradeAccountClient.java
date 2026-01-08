package com.myqyl.aitradex.etrade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import java.util.*;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Client for E*TRADE Account API endpoints.
 */
@Component
public class EtradeAccountClient {

  private static final Logger log = LoggerFactory.getLogger(EtradeAccountClient.class);

  private final EtradeApiClient apiClient;
  private final EtradeProperties properties;
  private final ObjectMapper objectMapper;

  public EtradeAccountClient(EtradeApiClient apiClient, EtradeProperties properties, 
                            ObjectMapper objectMapper) {
    this.apiClient = apiClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /**
   * Gets list of accounts for a user.
   */
  public List<Map<String, Object>> getAccountList(UUID accountId) {
    try {
      String url = properties.getAccountsListUrl();
      String response = apiClient.makeRequest("GET", url, null, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode accountsNode = root.path("AccountListResponse").path("Accounts").path("Account");
      
      List<Map<String, Object>> accounts = new ArrayList<>();
      if (accountsNode.isArray()) {
        for (JsonNode accountNode : accountsNode) {
          accounts.add(parseAccount(accountNode));
        }
      } else if (accountsNode.isObject()) {
        accounts.add(parseAccount(accountsNode));
      }
      
      return accounts;
    } catch (Exception e) {
      log.error("Failed to get account list", e);
      throw new RuntimeException("Failed to get account list", e);
    }
  }

  /**
   * Gets account balance.
   */
  public Map<String, Object> getBalance(UUID accountId, String accountIdKey) {
    try {
      String url = properties.getBalanceUrl(accountIdKey);
      Map<String, String> params = new HashMap<>();
      params.put("instType", "BROKERAGE");
      params.put("realTimeNAV", "true");
      
      String response = apiClient.makeRequest("GET", url, params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode balanceNode = root.path("BalanceResponse");
      
      return parseBalance(balanceNode);
    } catch (Exception e) {
      log.error("Failed to get balance for account {}", accountIdKey, e);
      throw new RuntimeException("Failed to get balance", e);
    }
  }

  /**
   * Gets account portfolio.
   */
  public Map<String, Object> getPortfolio(UUID accountId, String accountIdKey) {
    try {
      String url = properties.getPortfolioUrl(accountIdKey);
      String response = apiClient.makeRequest("GET", url, null, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode portfolioNode = root.path("PortfolioResponse");
      
      return parsePortfolio(portfolioNode);
    } catch (Exception e) {
      log.error("Failed to get portfolio for account {}", accountIdKey, e);
      throw new RuntimeException("Failed to get portfolio", e);
    }
  }

  private Map<String, Object> parseAccount(JsonNode accountNode) {
    Map<String, Object> account = new HashMap<>();
    account.put("accountIdKey", accountNode.path("accountIdKey").asText());
    account.put("accountId", accountNode.path("accountId").asText());
    account.put("accountName", accountNode.path("accountName").asText());
    account.put("accountType", accountNode.path("accountType").asText());
    account.put("accountDesc", accountNode.path("accountDesc").asText());
    account.put("accountStatus", accountNode.path("accountStatus").asText());
    return account;
  }

  private Map<String, Object> parseBalance(JsonNode balanceNode) {
    Map<String, Object> balance = new HashMap<>();
    balance.put("accountId", balanceNode.path("accountId").asText());
    balance.put("accountType", balanceNode.path("accountType").asText());
    
    JsonNode computedNode = balanceNode.path("Computed");
    if (!computedNode.isMissingNode()) {
      Map<String, Object> computed = new HashMap<>();
      computed.put("total", computedNode.path("total").asText());
      computed.put("netCash", computedNode.path("netCash").asText());
      computed.put("cashAvailableForInvestment", computedNode.path("cashAvailableForInvestment").asText());
      balance.put("computed", computed);
    }
    
    return balance;
  }

  private Map<String, Object> parsePortfolio(JsonNode portfolioNode) {
    Map<String, Object> portfolio = new HashMap<>();
    
    // Handle AccountPortfolio array structure (example app shows this pattern)
    JsonNode accountPortfolioNode = portfolioNode.path("AccountPortfolio");
    if (!accountPortfolioNode.isMissingNode()) {
      List<Map<String, Object>> accountPortfolios = new ArrayList<>();
      
      if (accountPortfolioNode.isArray()) {
        // Multiple account portfolios
        for (JsonNode acctPortfolio : accountPortfolioNode) {
          accountPortfolios.add(parseAccountPortfolio(acctPortfolio));
        }
      } else {
        // Single account portfolio
        accountPortfolios.add(parseAccountPortfolio(accountPortfolioNode));
      }
      
      // Merge all positions from all account portfolios
      List<Map<String, Object>> allPositions = new ArrayList<>();
      for (Map<String, Object> acctPortfolio : accountPortfolios) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> positions = (List<Map<String, Object>>) acctPortfolio.get("positions");
        if (positions != null) {
          allPositions.addAll(positions);
        }
        // Use first account ID as primary
        if (portfolio.get("accountId") == null) {
          portfolio.put("accountId", acctPortfolio.get("accountId"));
        }
      }
      portfolio.put("positions", allPositions);
    } else {
      // Fallback: handle direct Position array (older structure)
      portfolio.put("accountId", portfolioNode.path("accountId").asText(""));
      
      JsonNode positionsNode = portfolioNode.path("Position");
      List<Map<String, Object>> positions = new ArrayList<>();
      if (positionsNode.isArray()) {
        for (JsonNode positionNode : positionsNode) {
          positions.add(parsePosition(positionNode));
        }
      } else if (positionsNode.isObject()) {
        positions.add(parsePosition(positionsNode));
      }
      portfolio.put("positions", positions);
    }
    
    return portfolio;
  }

  private Map<String, Object> parseAccountPortfolio(JsonNode acctPortfolioNode) {
    Map<String, Object> accountPortfolio = new HashMap<>();
    accountPortfolio.put("accountId", acctPortfolioNode.path("accountId").asText(""));
    
    JsonNode positionsNode = acctPortfolioNode.path("Position");
    List<Map<String, Object>> positions = new ArrayList<>();
    if (positionsNode.isArray()) {
      for (JsonNode positionNode : positionsNode) {
        positions.add(parsePosition(positionNode));
      }
    } else if (positionsNode.isObject()) {
      positions.add(parsePosition(positionsNode));
    }
    accountPortfolio.put("positions", positions);
    
    return accountPortfolio;
  }

  private Map<String, Object> parsePosition(JsonNode positionNode) {
    Map<String, Object> position = new HashMap<>();
    
    // Product information
    JsonNode productNode = positionNode.path("Product");
    if (!productNode.isMissingNode()) {
      position.put("symbol", productNode.path("symbol").asText(""));
      position.put("securityType", productNode.path("securityType").asText(""));
      position.put("exchange", productNode.path("exchange").asText(""));
    } else {
      // Fallback: direct symbol field
      position.put("symbol", positionNode.path("symbol").asText(""));
    }
    
    // Quantity (handle both number and string)
    JsonNode quantityNode = positionNode.path("quantity");
    if (!quantityNode.isMissingNode()) {
      if (quantityNode.isNumber()) {
        position.put("quantity", quantityNode.asDouble());
      } else {
        position.put("quantity", Double.parseDouble(quantityNode.asText("0")));
      }
    }
    
    // Price information from Quick quote
    JsonNode quickNode = positionNode.path("Quick");
    if (!quickNode.isMissingNode()) {
      JsonNode lastTradeNode = quickNode.path("lastTrade");
      if (!lastTradeNode.isMissingNode()) {
        if (lastTradeNode.isNumber()) {
          position.put("lastTrade", lastTradeNode.asDouble());
        } else {
          position.put("lastTrade", Double.parseDouble(lastTradeNode.asText("0")));
        }
      }
    }
    
    // Price paid
    JsonNode pricePaidNode = positionNode.path("pricePaid");
    if (!pricePaidNode.isMissingNode()) {
      if (pricePaidNode.isNumber()) {
        position.put("pricePaid", pricePaidNode.asDouble());
      } else {
        position.put("pricePaid", Double.parseDouble(pricePaidNode.asText("0")));
      }
    }
    
    // Cost basis
    JsonNode costBasisNode = positionNode.path("costBasis");
    if (!costBasisNode.isMissingNode()) {
      position.put("costBasis", costBasisNode.asText(""));
    }
    
    // Total gain
    JsonNode totalGainNode = positionNode.path("totalGain");
    if (!totalGainNode.isMissingNode()) {
      if (totalGainNode.isNumber()) {
        position.put("totalGain", totalGainNode.asDouble());
      } else {
        position.put("totalGain", totalGainNode.asText(""));
      }
    }
    
    // Total gain percentage
    JsonNode totalGainPctNode = positionNode.path("totalGainPct");
    if (!totalGainPctNode.isMissingNode()) {
      if (totalGainPctNode.isNumber()) {
        position.put("totalGainPct", totalGainPctNode.asDouble());
      } else {
        position.put("totalGainPct", Double.parseDouble(totalGainPctNode.asText("0")));
      }
    }
    
    // Market value
    JsonNode marketValueNode = positionNode.path("marketValue");
    if (!marketValueNode.isMissingNode()) {
      if (marketValueNode.isNumber()) {
        position.put("marketValue", marketValueNode.asDouble());
      } else {
        position.put("marketValue", Double.parseDouble(marketValueNode.asText("0")));
      }
    }
    
    return position;
  }
}
