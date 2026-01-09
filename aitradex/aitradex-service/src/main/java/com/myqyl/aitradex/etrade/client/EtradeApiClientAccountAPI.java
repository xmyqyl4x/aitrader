package com.myqyl.aitradex.etrade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.accounts.dto.AccountListResponse;
import com.myqyl.aitradex.etrade.accounts.dto.AccountPortfolioDto;
import com.myqyl.aitradex.etrade.accounts.dto.BalanceRequest;
import com.myqyl.aitradex.etrade.accounts.dto.BalanceResponse;
import com.myqyl.aitradex.etrade.accounts.dto.CashBalance;
import com.myqyl.aitradex.etrade.accounts.dto.ComputedBalance;
import com.myqyl.aitradex.etrade.accounts.dto.EtradeAccountModel;
import com.myqyl.aitradex.etrade.accounts.dto.MarginBalance;
import com.myqyl.aitradex.etrade.accounts.dto.PortfolioRequest;
import com.myqyl.aitradex.etrade.accounts.dto.PortfolioResponse;
import com.myqyl.aitradex.etrade.accounts.dto.PositionDto;
import com.myqyl.aitradex.etrade.accounts.dto.ProductDto;
import com.myqyl.aitradex.etrade.accounts.dto.QuickViewDto;
import com.myqyl.aitradex.etrade.accounts.dto.TotalsDto;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * E*TRADE Accounts API Client.
 * 
 * This class refactors account-specific functionality from EtradeAccountClient
 * into a dedicated Accounts API layer.
 * 
 * Implements all 3 Accounts API endpoints as per E*TRADE Accounts API documentation:
 * 1. List Accounts
 * 2. Get Account Balances
 * 3. View Portfolio
 * 
 * All request and response objects are DTOs/Models, not Maps, as per requirements.
 */
@Component
public class EtradeApiClientAccountAPI {

  private static final Logger log = LoggerFactory.getLogger(EtradeApiClientAccountAPI.class);

  private final EtradeApiClient apiClient;
  private final EtradeProperties properties;
  private final ObjectMapper objectMapper;

  public EtradeApiClientAccountAPI(
      EtradeApiClient apiClient,
      EtradeProperties properties,
      ObjectMapper objectMapper) {
    this.apiClient = apiClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /**
   * 1. List Accounts
   * 
   * Returns a list of E*TRADE accounts for the current user.
   * 
   * @param accountId Internal account UUID for authentication
   * @return AccountListResponse DTO containing list of accounts
   * @throws EtradeApiException if the request fails
   */
  public AccountListResponse listAccounts(UUID accountId) {
    try {
      String url = properties.getAccountsListUrl();
      String response = apiClient.makeRequest("GET", url, null, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode accountsNode = root.path("AccountListResponse").path("Accounts");
      
      List<EtradeAccountModel> accounts = new ArrayList<>();
      JsonNode accountArray = accountsNode.path("Account");
      
      if (accountArray.isArray()) {
        for (JsonNode accountNode : accountArray) {
          accounts.add(parseAccount(accountNode));
        }
      } else if (accountArray.isObject() && !accountArray.isMissingNode()) {
        accounts.add(parseAccount(accountArray));
      }
      
      log.debug("Retrieved {} accounts for account ID {}", accounts.size(), accountId);
      
      AccountListResponse.Accounts accountsContainer = new AccountListResponse.Accounts(accounts);
      return new AccountListResponse(accountsContainer);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to list accounts", e);
      throw new EtradeApiException(500, "LIST_ACCOUNTS_FAILED", 
          "Failed to list accounts: " + e.getMessage(), e);
    }
  }

  /**
   * 2. Get Account Balances
   * 
   * Retrieves the current account balance and related details for a specified account.
   * 
   * @param accountId Internal account UUID for authentication
   * @param accountIdKey E*TRADE account ID key
   * @param request BalanceRequest DTO containing query parameters
   * @return BalanceResponse DTO containing balance information
   * @throws EtradeApiException if the request fails
   */
  public BalanceResponse getAccountBalance(UUID accountId, String accountIdKey, BalanceRequest request) {
    try {
      String url = properties.getBalanceUrl(accountIdKey);
      Map<String, String> params = new HashMap<>();
      
      if (request.getInstType() != null) {
        params.put("instType", request.getInstType());
      }
      if (request.getAccountType() != null && !request.getAccountType().isEmpty()) {
        params.put("accountType", request.getAccountType());
      }
      if (request.getRealTimeNAV() != null) {
        params.put("realTimeNAV", String.valueOf(request.getRealTimeNAV()));
      }
      
      String response = apiClient.makeRequest("GET", url, params.isEmpty() ? null : params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode balanceNode = root.path("BalanceResponse");
      
      return parseBalance(balanceNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to get balance for account {}", accountIdKey, e);
      throw new EtradeApiException(500, "GET_BALANCE_FAILED", 
          "Failed to get account balance: " + e.getMessage(), e);
    }
  }

  /**
   * 3. View Portfolio
   * 
   * Returns portfolio information for a specified account.
   * 
   * @param accountId Internal account UUID for authentication
   * @param accountIdKey E*TRADE account ID key
   * @param request PortfolioRequest DTO containing query parameters
   * @return PortfolioResponse DTO containing portfolio information
   * @throws EtradeApiException if the request fails
   */
  public PortfolioResponse viewPortfolio(UUID accountId, String accountIdKey, PortfolioRequest request) {
    try {
      String url = properties.getPortfolioUrl(accountIdKey);
      Map<String, String> params = new HashMap<>();
      
      if (request.getCount() != null && request.getCount() > 0) {
        params.put("count", String.valueOf(request.getCount()));
      }
      if (request.getSortBy() != null && !request.getSortBy().isEmpty()) {
        params.put("sortBy", request.getSortBy());
      }
      if (request.getSortOrder() != null && !request.getSortOrder().isEmpty()) {
        params.put("sortOrder", request.getSortOrder());
      }
      if (request.getPageNumber() != null && request.getPageNumber() > 0) {
        params.put("pageNumber", String.valueOf(request.getPageNumber()));
      }
      if (request.getMarketSession() != null && !request.getMarketSession().isEmpty()) {
        params.put("marketSession", request.getMarketSession());
      }
      if (request.getTotalsRequired() != null) {
        params.put("totalsRequired", String.valueOf(request.getTotalsRequired()));
      }
      if (request.getLotsRequired() != null) {
        params.put("lotsRequired", String.valueOf(request.getLotsRequired()));
      }
      if (request.getView() != null && !request.getView().isEmpty()) {
        params.put("view", request.getView());
      }
      
      String response = apiClient.makeRequest("GET", url, params.isEmpty() ? null : params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode portfolioResponseNode = root.path("PortfolioResponse");
      
      return parsePortfolio(portfolioResponseNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to get portfolio for account {}", accountIdKey, e);
      throw new EtradeApiException(500, "GET_PORTFOLIO_FAILED", 
          "Failed to get portfolio: " + e.getMessage(), e);
    }
  }

  /**
   * Parses Account JSON node into EtradeAccountModel.
   */
  private EtradeAccountModel parseAccount(JsonNode accountNode) {
    EtradeAccountModel account = new EtradeAccountModel();
    account.setAccountNo(getIntValue(accountNode, "accountNo"));
    account.setAccountId(getStringValue(accountNode, "accountId"));
    account.setAccountIdKey(getStringValue(accountNode, "accountIdKey"));
    account.setAccountMode(getStringValue(accountNode, "accountMode"));
    account.setAccountDesc(getStringValue(accountNode, "accountDesc"));
    account.setAccountName(getStringValue(accountNode, "accountName"));
    account.setAccountType(getStringValue(accountNode, "accountType"));
    account.setInstitutionType(getStringValue(accountNode, "institutionType"));
    account.setAccountStatus(getStringValue(accountNode, "accountStatus"));
    account.setClosedDate(getLongValue(accountNode, "closedDate"));
    account.setShareWorksAccount(accountNode.path("ShareWorksAccount").asBoolean(false));
    account.setShareWorksSource(getStringValue(accountNode, "ShareWorksSource"));
    account.setFCManagedMssbClosedAccount(accountNode.path("fCManagedMssbClosedAccount").asBoolean(false));
    return account;
  }

  /**
   * Parses Balance JSON node into BalanceResponse DTO.
   */
  private BalanceResponse parseBalance(JsonNode balanceNode) {
    BalanceResponse balance = new BalanceResponse();
    balance.setAccountId(getStringValue(balanceNode, "accountId"));
    balance.setAccountType(getStringValue(balanceNode, "accountType"));
    balance.setAccountDescription(getStringValue(balanceNode, "accountDescription"));
    balance.setAccountMode(getStringValue(balanceNode, "accountMode"));
    
    // Parse Cash section
    JsonNode cashNode = balanceNode.path("Cash");
    if (!cashNode.isMissingNode()) {
      CashBalance cash = new CashBalance();
      cash.setCashBalance(getDoubleValue(cashNode, "cashBalance"));
      cash.setCashAvailable(getDoubleValue(cashNode, "cashAvailable"));
      cash.setUnclearedDeposits(getDoubleValue(cashNode, "unclearedDeposits"));
      cash.setCashSweep(getDoubleValue(cashNode, "cashSweep"));
      balance.setCash(cash);
    }
    
    // Parse Margin section
    JsonNode marginNode = balanceNode.path("Margin");
    if (!marginNode.isMissingNode()) {
      MarginBalance margin = new MarginBalance();
      margin.setMarginBalance(getDoubleValue(marginNode, "marginBalance"));
      margin.setMarginAvailable(getDoubleValue(marginNode, "marginAvailable"));
      margin.setMarginBuyingPower(getDoubleValue(marginNode, "marginBuyingPower"));
      margin.setDayTradingBuyingPower(getDoubleValue(marginNode, "dayTradingBuyingPower"));
      balance.setMargin(margin);
    }
    
    // Parse Computed section
    JsonNode computedNode = balanceNode.path("Computed");
    if (!computedNode.isMissingNode()) {
      ComputedBalance computed = new ComputedBalance();
      computed.setTotal(getDoubleValue(computedNode, "total"));
      computed.setNetCash(getDoubleValue(computedNode, "netCash"));
      computed.setCashAvailableForInvestment(getDoubleValue(computedNode, "cashAvailableForInvestment"));
      computed.setTotalValue(getDoubleValue(computedNode, "totalValue"));
      computed.setNetValue(getDoubleValue(computedNode, "netValue"));
      computed.setSettledCash(getDoubleValue(computedNode, "settledCash"));
      computed.setOpenCalls(getDoubleValue(computedNode, "openCalls"));
      computed.setOpenPuts(getDoubleValue(computedNode, "openPuts"));
      balance.setComputed(computed);
    }
    
    return balance;
  }

  /**
   * Parses Portfolio JSON node into PortfolioResponse DTO.
   */
  private PortfolioResponse parsePortfolio(JsonNode portfolioResponseNode) {
    PortfolioResponse portfolio = new PortfolioResponse();
    
    // Parse totalPages
    portfolio.setTotalPages(getIntValue(portfolioResponseNode, "totalPages"));
    
    // Handle AccountPortfolio array structure
    JsonNode accountPortfolioNode = portfolioResponseNode.path("AccountPortfolio");
    if (!accountPortfolioNode.isMissingNode()) {
      List<AccountPortfolioDto> accountPortfolios = new ArrayList<>();
      
      if (accountPortfolioNode.isArray()) {
        for (JsonNode acctPortfolio : accountPortfolioNode) {
          accountPortfolios.add(parseAccountPortfolio(acctPortfolio));
        }
      } else {
        accountPortfolios.add(parseAccountPortfolio(accountPortfolioNode));
      }
      
      portfolio.setAccountPortfolios(accountPortfolios);
    }
    
    return portfolio;
  }

  /**
   * Parses AccountPortfolio JSON node into AccountPortfolioDto.
   */
  private AccountPortfolioDto parseAccountPortfolio(JsonNode acctPortfolioNode) {
    AccountPortfolioDto accountPortfolio = new AccountPortfolioDto();
    accountPortfolio.setAccountId(getStringValue(acctPortfolioNode, "accountId"));
    accountPortfolio.setTotalPages(getIntValue(acctPortfolioNode, "totalPages"));
    
    // Parse positions
    JsonNode positionsNode = acctPortfolioNode.path("Position");
    List<PositionDto> positions = new ArrayList<>();
    if (positionsNode.isArray()) {
      for (JsonNode positionNode : positionsNode) {
        positions.add(parsePosition(positionNode));
      }
    } else if (positionsNode.isObject() && !positionsNode.isMissingNode()) {
      positions.add(parsePosition(positionsNode));
    }
    accountPortfolio.setPositions(positions);
    
    // Parse totals if present
    JsonNode totalsNode = acctPortfolioNode.path("Totals");
    if (!totalsNode.isMissingNode()) {
      accountPortfolio.setTotals(parseTotals(totalsNode));
    }
    
    return accountPortfolio;
  }

  /**
   * Parses Position JSON node into PositionDto.
   */
  private PositionDto parsePosition(JsonNode positionNode) {
    PositionDto position = new PositionDto();
    
    position.setPositionId(getLongValue(positionNode, "positionId"));
    position.setSymbolDescription(getStringValue(positionNode, "symbolDescription"));
    position.setDateAcquired(getLongValue(positionNode, "dateAcquired"));
    position.setPricePaid(getDoubleValue(positionNode, "pricePaid"));
    position.setCommissions(getDoubleValue(positionNode, "commissions"));
    position.setOtherFees(getDoubleValue(positionNode, "otherFees"));
    position.setQuantity(getDoubleValue(positionNode, "quantity"));
    position.setPositionIndicator(getStringValue(positionNode, "positionIndicator"));
    position.setPositionType(getStringValue(positionNode, "positionType"));
    position.setDaysGain(getDoubleValue(positionNode, "daysGain"));
    position.setDaysGainPct(getDoubleValue(positionNode, "daysGainPct"));
    position.setMarketValue(getDoubleValue(positionNode, "marketValue"));
    position.setTotalCost(getDoubleValue(positionNode, "totalCost"));
    position.setTotalGain(getDoubleValue(positionNode, "totalGain"));
    position.setTotalGainPct(getDoubleValue(positionNode, "totalGainPct"));
    position.setPctOfPortfolio(getDoubleValue(positionNode, "pctOfPortfolio"));
    position.setCostPerShare(getDoubleValue(positionNode, "costPerShare"));
    position.setTodayCommissions(getDoubleValue(positionNode, "todayCommissions"));
    position.setTodayFees(getDoubleValue(positionNode, "todayFees"));
    position.setTodayPricePaid(getDoubleValue(positionNode, "todayPricePaid"));
    position.setTodayQuantity(getDoubleValue(positionNode, "todayQuantity"));
    position.setAdjPrevClose(getDoubleValue(positionNode, "adjPrevClose"));
    position.setLotsDetails(getStringValue(positionNode, "lotsDetails"));
    position.setQuoteDetails(getStringValue(positionNode, "quoteDetails"));
    
    // Additional fields
    position.setCusip(getStringValue(positionNode, "cusip"));
    position.setExchange(getStringValue(positionNode, "exchange"));
    position.setIsQuotable(positionNode.path("isQuotable").asBoolean(false));
    position.setGainLoss(getDoubleValue(positionNode, "gainLoss"));
    position.setGainLossPercent(getDoubleValue(positionNode, "gainLossPercent"));
    position.setCostBasis(getDoubleValue(positionNode, "costBasis"));
    position.setIntrinsicValue(getDoubleValue(positionNode, "intrinsicValue"));
    position.setTimeValue(getDoubleValue(positionNode, "timeValue"));
    position.setMultiplier(getIntValue(positionNode, "multiplier"));
    position.setDigits(getIntValue(positionNode, "digits"));
    
    // Parse Product
    JsonNode productNode = positionNode.path("Product");
    if (!productNode.isMissingNode()) {
      position.setProduct(parseProduct(productNode));
    }
    
    // Parse QuickView
    JsonNode quickNode = positionNode.path("Quick");
    if (!quickNode.isMissingNode()) {
      position.setQuick(parseQuickView(quickNode));
    }
    
    return position;
  }

  /**
   * Parses Product JSON node into ProductDto.
   */
  private ProductDto parseProduct(JsonNode productNode) {
    ProductDto product = new ProductDto();
    product.setSymbol(getStringValue(productNode, "symbol"));
    product.setSecurityType(getStringValue(productNode, "securityType"));
    product.setSecuritySubType(getStringValue(productNode, "securitySubType"));
    product.setCallPut(getStringValue(productNode, "callPut"));
    product.setExpiryYear(getIntValue(productNode, "expiryYear"));
    product.setExpiryMonth(getIntValue(productNode, "expiryMonth"));
    product.setExpiryDay(getIntValue(productNode, "expiryDay"));
    product.setStrikePrice(getDoubleValue(productNode, "strikePrice"));
    product.setExpiryType(getStringValue(productNode, "expiryType"));
    
    // Parse ProductId
    JsonNode productIdNode = productNode.path("productId");
    if (!productIdNode.isMissingNode()) {
      ProductDto.ProductIdDto productId = new ProductDto.ProductIdDto();
      productId.setSymbol(getStringValue(productIdNode, "symbol"));
      productId.setTypeCode(getStringValue(productIdNode, "typeCode"));
      product.setProductId(productId);
    }
    
    return product;
  }

  /**
   * Parses QuickView JSON node into QuickViewDto.
   */
  private QuickViewDto parseQuickView(JsonNode quickNode) {
    QuickViewDto quick = new QuickViewDto();
    quick.setLastTrade(getDoubleValue(quickNode, "lastTrade"));
    quick.setLastTradeTime(getLongValue(quickNode, "lastTradeTime"));
    quick.setChange(getDoubleValue(quickNode, "change"));
    quick.setChangePct(getDoubleValue(quickNode, "changePct"));
    quick.setVolume(getLongValue(quickNode, "volume"));
    quick.setQuoteStatus(getStringValue(quickNode, "quoteStatus"));
    quick.setSevenDayCurrentYield(getDoubleValue(quickNode, "sevenDayCurrentYield"));
    quick.setAnnualTotalReturn(getDoubleValue(quickNode, "annualTotalReturn"));
    quick.setWeightedAverageMaturity(getDoubleValue(quickNode, "weightedAverageMaturity"));
    return quick;
  }

  /**
   * Parses Totals JSON node into TotalsDto.
   */
  private TotalsDto parseTotals(JsonNode totalsNode) {
    TotalsDto totals = new TotalsDto();
    totals.setTodaysGainLoss(getDoubleValue(totalsNode, "todaysGainLoss"));
    totals.setTodaysGainLossPct(getDoubleValue(totalsNode, "todaysGainLossPct"));
    totals.setTotalGainLossPct(getDoubleValue(totalsNode, "totalGainLossPct"));
    totals.setTotalMarketValue(getDoubleValue(totalsNode, "totalMarketValue"));
    totals.setTotalGainLoss(getDoubleValue(totalsNode, "totalGainLoss"));
    totals.setTotalPricePaid(getDoubleValue(totalsNode, "totalPricePaid"));
    totals.setCashBalance(getDoubleValue(totalsNode, "cashBalance"));
    return totals;
  }

  // Helper methods for parsing JSON values

  private String getStringValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    String text = fieldNode.asText();
    return (text != null && !text.isEmpty()) ? text : null;
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

  private Long getLongValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asLong();
    }
    try {
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Long.parseLong(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
