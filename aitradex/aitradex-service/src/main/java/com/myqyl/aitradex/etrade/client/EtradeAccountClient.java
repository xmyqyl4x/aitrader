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
   * 
   * @param accountId Internal account UUID
   * @param accountIdKey E*TRADE account ID key
   * @param instType Institution type (default: "BROKERAGE")
   * @param accountType Account type filter (optional)
   * @param realTimeNAV Whether to get real-time NAV (default: true)
   */
  public Map<String, Object> getBalance(UUID accountId, String accountIdKey, 
                                         String instType, String accountType, Boolean realTimeNAV) {
    try {
      String url = properties.getBalanceUrl(accountIdKey);
      Map<String, String> params = new HashMap<>();
      params.put("instType", instType != null ? instType : "BROKERAGE");
      if (accountType != null && !accountType.isEmpty()) {
        params.put("accountType", accountType);
      }
      params.put("realTimeNAV", realTimeNAV != null ? String.valueOf(realTimeNAV) : "true");
      
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
   * Gets account balance (simplified version with defaults).
   */
  public Map<String, Object> getBalance(UUID accountId, String accountIdKey) {
    return getBalance(accountId, accountIdKey, null, null, null);
  }

  /**
   * Gets account portfolio.
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
   */
  public Map<String, Object> getPortfolio(UUID accountId, String accountIdKey, Integer count,
                                           String sortBy, String sortOrder, Integer pageNumber,
                                           String marketSession, Boolean totalsRequired,
                                           Boolean lotsRequired, String view) {
    try {
      String url = properties.getPortfolioUrl(accountIdKey);
      Map<String, String> params = new HashMap<>();
      
      if (count != null && count > 0) {
        params.put("count", String.valueOf(count));
      }
      if (sortBy != null && !sortBy.isEmpty()) {
        params.put("sortBy", sortBy);
      }
      if (sortOrder != null && !sortOrder.isEmpty()) {
        params.put("sortOrder", sortOrder);
      }
      if (pageNumber != null && pageNumber > 0) {
        params.put("pageNumber", String.valueOf(pageNumber));
      }
      if (marketSession != null && !marketSession.isEmpty()) {
        params.put("marketSession", marketSession);
      }
      if (totalsRequired != null) {
        params.put("totalsRequired", String.valueOf(totalsRequired));
      }
      if (lotsRequired != null) {
        params.put("lotsRequired", String.valueOf(lotsRequired));
      }
      if (view != null && !view.isEmpty()) {
        params.put("view", view);
      }
      
      String response = apiClient.makeRequest("GET", url, params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode portfolioNode = root.path("PortfolioResponse");
      
      return parsePortfolio(portfolioNode);
    } catch (Exception e) {
      log.error("Failed to get portfolio for account {}", accountIdKey, e);
      throw new RuntimeException("Failed to get portfolio", e);
    }
  }

  /**
   * Gets account portfolio (simplified version with defaults).
   */
  public Map<String, Object> getPortfolio(UUID accountId, String accountIdKey) {
    return getPortfolio(accountId, accountIdKey, null, null, null, null, null, null, null, null);
  }

  private Map<String, Object> parseAccount(JsonNode accountNode) {
    Map<String, Object> account = new HashMap<>();
    account.put("accountIdKey", accountNode.path("accountIdKey").asText());
    account.put("accountId", accountNode.path("accountId").asText());
    account.put("accountName", accountNode.path("accountName").asText());
    account.put("accountType", accountNode.path("accountType").asText());
    account.put("accountDesc", accountNode.path("accountDesc").asText());
    account.put("accountStatus", accountNode.path("accountStatus").asText());
    account.put("accountMode", accountNode.path("accountMode").asText(""));
    account.put("institutionType", accountNode.path("institutionType").asText(""));
    return account;
  }

  private Map<String, Object> parseBalance(JsonNode balanceNode) {
    Map<String, Object> balance = new HashMap<>();
    balance.put("accountId", balanceNode.path("accountId").asText());
    balance.put("accountType", balanceNode.path("accountType").asText());
    balance.put("accountDescription", balanceNode.path("accountDescription").asText(""));
    balance.put("accountMode", balanceNode.path("accountMode").asText(""));
    
    // Cash section
    JsonNode cashNode = balanceNode.path("Cash");
    if (!cashNode.isMissingNode()) {
      Map<String, Object> cash = new HashMap<>();
      cash.put("cashBalance", getDoubleValue(cashNode, "cashBalance"));
      cash.put("cashAvailable", getDoubleValue(cashNode, "cashAvailable"));
      cash.put("unclearedDeposits", getDoubleValue(cashNode, "unclearedDeposits"));
      cash.put("cashSweep", getDoubleValue(cashNode, "cashSweep"));
      balance.put("cash", cash);
    }
    
    // Margin section
    JsonNode marginNode = balanceNode.path("Margin");
    if (!marginNode.isMissingNode()) {
      Map<String, Object> margin = new HashMap<>();
      margin.put("marginBalance", getDoubleValue(marginNode, "marginBalance"));
      margin.put("marginAvailable", getDoubleValue(marginNode, "marginAvailable"));
      margin.put("marginBuyingPower", getDoubleValue(marginNode, "marginBuyingPower"));
      margin.put("dayTradingBuyingPower", getDoubleValue(marginNode, "dayTradingBuyingPower"));
      balance.put("margin", margin);
    }
    
    // Computed section (enhanced)
    JsonNode computedNode = balanceNode.path("Computed");
    if (!computedNode.isMissingNode()) {
      Map<String, Object> computed = new HashMap<>();
      computed.put("total", getDoubleValue(computedNode, "total"));
      computed.put("netCash", getDoubleValue(computedNode, "netCash"));
      computed.put("cashAvailableForInvestment", getDoubleValue(computedNode, "cashAvailableForInvestment"));
      computed.put("totalValue", getDoubleValue(computedNode, "totalValue"));
      computed.put("netValue", getDoubleValue(computedNode, "netValue"));
      computed.put("settledCash", getDoubleValue(computedNode, "settledCash"));
      computed.put("openCalls", getDoubleValue(computedNode, "openCalls"));
      computed.put("openPuts", getDoubleValue(computedNode, "openPuts"));
      balance.put("computed", computed);
    }
    
    return balance;
  }

  private Double getDoubleValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asDouble();
    }
    try {
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Map<String, Object> parsePortfolio(JsonNode portfolioNode) {
    Map<String, Object> portfolio = new HashMap<>();
    
    // Parse totalPages
    JsonNode totalPagesNode = portfolioNode.path("totalPages");
    if (!totalPagesNode.isMissingNode()) {
      portfolio.put("totalPages", totalPagesNode.asInt(0));
    }
    
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

  /**
   * Gets list of transactions for an account.
   * 
   * @param accountId Internal account UUID
   * @param accountIdKey E*TRADE account ID key
   * @param marker Pagination marker (optional)
   * @param count Number of transactions to return (optional)
   * @param startDate Start date filter (MMddyyyy format) (optional)
   * @param endDate End date filter (MMddyyyy format) (optional)
   * @param sortOrder Sort direction ("ASC", "DESC") (optional)
   * @param accept Response format ("xml" or "json") (optional)
   * @param storeId Store ID filter (optional)
   */
  public Map<String, Object> getTransactions(UUID accountId, String accountIdKey,
      String marker, Integer count, String startDate, String endDate, String sortOrder,
      String accept, String storeId) {
    try {
      String url = properties.getTransactionsUrl(accountIdKey);
      Map<String, String> params = new HashMap<>();
      if (marker != null && !marker.isEmpty()) {
        params.put("marker", marker);
      }
      if (count != null && count > 0) {
        params.put("count", String.valueOf(count));
      }
      if (startDate != null && !startDate.isEmpty()) {
        params.put("startDate", startDate);
      }
      if (endDate != null && !endDate.isEmpty()) {
        params.put("endDate", endDate);
      }
      if (sortOrder != null && !sortOrder.isEmpty()) {
        params.put("sortOrder", sortOrder);
      }
      if (accept != null && !accept.isEmpty()) {
        params.put("accept", accept);
      }
      if (storeId != null && !storeId.isEmpty()) {
        params.put("storeId", storeId);
      }
      
      String response = apiClient.makeRequest("GET", url, params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode responseNode = root.path("TransactionListResponse");
      
      // Parse metadata
      Map<String, Object> result = new HashMap<>();
      result.put("transactionCount", getIntValue(responseNode, "transactionCount"));
      result.put("totalCount", getIntValue(responseNode, "totalCount"));
      result.put("moreTransactions", responseNode.path("moreTransactions").asBoolean(false));
      JsonNode nextNode = responseNode.path("next");
      if (!nextNode.isMissingNode()) {
        result.put("next", nextNode.asText(""));
      }
      JsonNode markerNode = responseNode.path("marker");
      if (!markerNode.isMissingNode()) {
        result.put("marker", markerNode.asText(""));
      }
      
      // Parse transactions
      JsonNode transactionsNode = responseNode.path("Transactions").path("Transaction");
      List<Map<String, Object>> transactions = new ArrayList<>();
      if (transactionsNode.isArray()) {
        for (JsonNode transactionNode : transactionsNode) {
          transactions.add(parseTransaction(transactionNode));
        }
      } else if (transactionsNode.isObject()) {
        transactions.add(parseTransaction(transactionsNode));
      }
      result.put("transactions", transactions);
      
      return result;
    } catch (Exception e) {
      log.error("Failed to get transactions for account {}", accountIdKey, e);
      throw new RuntimeException("Failed to get transactions", e);
    }
  }

  /**
   * Gets list of transactions for an account (simplified version).
   */
  public List<Map<String, Object>> getTransactions(UUID accountId, String accountIdKey,
      String marker, Integer count) {
    Map<String, Object> result = getTransactions(accountId, accountIdKey, marker, count,
                                                  null, null, null, null, null);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("transactions");
    return transactions != null ? transactions : new ArrayList<>();
  }

  private Integer getIntValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asInt();
    }
    try {
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Integer.parseInt(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Gets transaction details by transaction ID.
   * 
   * @param accountId Internal account UUID
   * @param accountIdKey E*TRADE account ID key
   * @param transactionId Transaction ID
   * @param accept Response format ("xml" or "json") (optional)
   * @param storeId Store ID filter (optional)
   */
  public Map<String, Object> getTransactionDetails(UUID accountId, String accountIdKey, String transactionId,
                                                     String accept, String storeId) {
    try {
      String url = properties.getTransactionDetailsUrl(accountIdKey, transactionId);
      Map<String, String> params = new HashMap<>();
      if (accept != null && !accept.isEmpty()) {
        params.put("accept", accept);
      }
      if (storeId != null && !storeId.isEmpty()) {
        params.put("storeId", storeId);
      }
      
      String response = apiClient.makeRequest("GET", url, params.isEmpty() ? null : params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode transactionNode = root.path("TransactionDetailsResponse");
      
      return parseTransactionDetails(transactionNode);
    } catch (Exception e) {
      log.error("Failed to get transaction details for transaction {}", transactionId, e);
      throw new RuntimeException("Failed to get transaction details", e);
    }
  }

  /**
   * Gets transaction details by transaction ID (simplified version).
   */
  public Map<String, Object> getTransactionDetails(UUID accountId, String accountIdKey, String transactionId) {
    return getTransactionDetails(accountId, accountIdKey, transactionId, null, null);
  }

  private Map<String, Object> parseTransaction(JsonNode transactionNode) {
    Map<String, Object> transaction = new HashMap<>();
    transaction.put("transactionId", transactionNode.path("transactionId").asText(""));
    transaction.put("accountId", transactionNode.path("accountId").asText(""));
    transaction.put("transactionDate", transactionNode.path("transactionDate").asText(""));
    transaction.put("amount", transactionNode.path("amount").asText(""));
    transaction.put("description", transactionNode.path("description").asText(""));
    transaction.put("transactionType", transactionNode.path("transactionType").asText(""));
    transaction.put("instType", transactionNode.path("instType").asText(""));
    transaction.put("detailsURI", transactionNode.path("detailsURI").asText(""));
    return transaction;
  }

  private Map<String, Object> parseTransactionDetails(JsonNode transactionNode) {
    Map<String, Object> details = new HashMap<>();
    details.put("transactionId", transactionNode.path("transactionId").asText(""));
    details.put("accountId", transactionNode.path("accountId").asText(""));
    details.put("transactionDate", transactionNode.path("transactionDate").asText(""));
    details.put("amount", transactionNode.path("amount").asText(""));
    details.put("description", transactionNode.path("description").asText(""));
    
    JsonNode categoryNode = transactionNode.path("Category");
    if (!categoryNode.isMissingNode()) {
      Map<String, Object> category = new HashMap<>();
      category.put("categoryId", categoryNode.path("categoryId").asText(""));
      category.put("parentId", categoryNode.path("parentId").asText(""));
      details.put("category", category);
    }
    
    JsonNode brokerageNode = transactionNode.path("Brokerage");
    if (!brokerageNode.isMissingNode()) {
      Map<String, Object> brokerage = new HashMap<>();
      brokerage.put("transactionType", brokerageNode.path("transactionType").asText(""));
      details.put("brokerage", brokerage);
    }
    
    return details;
  }
}
