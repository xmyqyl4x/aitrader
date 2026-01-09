package com.myqyl.aitradex.etrade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.client.EtradeApiClientMarketAPI;
import com.myqyl.aitradex.etrade.client.EtradeQuoteClient;
import com.myqyl.aitradex.etrade.domain.*;
import com.myqyl.aitradex.etrade.market.dto.*;
import com.myqyl.aitradex.etrade.repository.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for E*TRADE quote operations.
 * 
 * Refactored to use DTOs/Models instead of Maps.
 * New methods use EtradeApiClientMarketAPI with DTOs.
 * Old methods are deprecated and delegate to new methods where possible.
 */
@Service
public class EtradeQuoteService {

  private static final Logger log = LoggerFactory.getLogger(EtradeQuoteService.class);

  private final EtradeQuoteClient quoteClient; // Deprecated - use marketApi instead
  private final EtradeApiClientMarketAPI marketApi; // New API client with DTOs
  private final EtradeLookupProductRepository lookupProductRepository;
  private final EtradeQuoteSnapshotRepository quoteSnapshotRepository;
  private final EtradeOptionExpireDateRepository optionExpireDateRepository;
  private final EtradeOptionChainSnapshotRepository optionChainSnapshotRepository;
  private final EtradeOptionContractRepository optionContractRepository;
  private final ObjectMapper objectMapper;

  public EtradeQuoteService(
      EtradeQuoteClient quoteClient,
      EtradeApiClientMarketAPI marketApi,
      EtradeLookupProductRepository lookupProductRepository,
      EtradeQuoteSnapshotRepository quoteSnapshotRepository,
      EtradeOptionExpireDateRepository optionExpireDateRepository,
      EtradeOptionChainSnapshotRepository optionChainSnapshotRepository,
      EtradeOptionContractRepository optionContractRepository,
      ObjectMapper objectMapper) {
    this.quoteClient = quoteClient;
    this.marketApi = marketApi;
    this.lookupProductRepository = lookupProductRepository;
    this.quoteSnapshotRepository = quoteSnapshotRepository;
    this.optionExpireDateRepository = optionExpireDateRepository;
    this.optionChainSnapshotRepository = optionChainSnapshotRepository;
    this.optionContractRepository = optionContractRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Gets quotes for one or more symbols using DTOs and persists quote snapshots.
   * 
   * @param accountId Internal account UUID (null for delayed quotes)
   * @param request GetQuotesRequest DTO containing symbols and query parameters
   * @return QuoteResponse DTO containing list of quotes
   */
  @Transactional
  public QuoteResponse getQuotes(UUID accountId, GetQuotesRequest request) {
    QuoteResponse response = marketApi.getQuotes(accountId, request);
    
    // Persist quote snapshots (append-only time-series)
    persistQuoteSnapshots(response, request.getDetailFlag());
    
    return response;
  }

  /**
   * Gets quotes for one or more symbols (deprecated - uses Maps).
   * @deprecated Use {@link #getQuotes(UUID, GetQuotesRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> getQuotes(UUID accountId, String[] symbols, String detailFlag,
                                              Boolean requireEarningsDate, Integer overrideSymbolCount,
                                              Boolean skipMiniOptionsCheck) {
    return quoteClient.getQuotes(accountId, symbols, detailFlag, requireEarningsDate, 
                                 overrideSymbolCount, skipMiniOptionsCheck);
  }

  /**
   * Gets quotes for one or more symbols (deprecated - simplified version with defaults).
   * @deprecated Use {@link #getQuotes(UUID, GetQuotesRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> getQuotes(UUID accountId, String... symbols) {
    return quoteClient.getQuotes(accountId, symbols);
  }

  /**
   * Gets a single quote using DTOs and persists quote snapshot.
   * 
   * @param accountId Internal account UUID (null for delayed quotes)
   * @param symbol Stock symbol
   * @param detailFlag Detail flag (optional)
   * @return EtradeQuoteModel DTO containing quote details
   */
  @Transactional
  public EtradeQuoteModel getQuote(UUID accountId, String symbol, String detailFlag) {
    GetQuotesRequest request = new GetQuotesRequest();
    request.setSymbols(symbol);
    if (detailFlag != null) {
      request.setDetailFlag(detailFlag);
    }
    
    QuoteResponse response = getQuotes(accountId, request); // This will persist snapshots
    if (response.getQuoteData().isEmpty()) {
      throw new RuntimeException("Quote not found for symbol: " + symbol);
    }
    return response.getQuoteData().get(0);
  }

  /**
   * Gets a single quote (deprecated - uses Maps).
   * @deprecated Use {@link #getQuote(UUID, String, String)} instead
   */
  @Deprecated
  public Map<String, Object> getQuote(UUID accountId, String symbol) {
    List<Map<String, Object>> quotes = quoteClient.getQuotes(accountId, symbol);
    if (quotes.isEmpty()) {
      throw new RuntimeException("Quote not found for symbol: " + symbol);
    }
    return quotes.get(0);
  }

  /**
   * Looks up products by symbol or company name using DTOs and persists lookup products.
   * 
   * @param request LookupProductRequest DTO containing search input
   * @return LookupProductResponse DTO containing list of matching products
   */
  @Transactional
  public LookupProductResponse lookupProduct(LookupProductRequest request) {
    LookupProductResponse response = marketApi.lookupProduct(request);
    
    // Persist lookup products (upsert by symbol+type)
    persistLookupProducts(response);
    
    return response;
  }

  /**
   * Looks up products by symbol or company name (deprecated - uses Maps).
   * @deprecated Use {@link #lookupProduct(LookupProductRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> lookupProduct(String input) {
    return quoteClient.lookupProduct(input);
  }

  /**
   * Gets option chains for a symbol using DTOs and persists chain snapshot and option contracts.
   * 
   * @param request GetOptionChainsRequest DTO containing symbol and option chain parameters
   * @return OptionChainResponse DTO containing option chains
   */
  @Transactional
  public OptionChainResponse getOptionChains(GetOptionChainsRequest request) {
    OptionChainResponse response = marketApi.getOptionChains(request);
    
    // Persist option chain snapshot (append-only)
    persistOptionChainSnapshot(response, request);
    
    // Persist option contracts (upsert by optionSymbol)
    persistOptionContracts(response);
    
    return response;
  }

  /**
   * Gets option chains for a symbol (deprecated - uses Maps).
   * @deprecated Use {@link #getOptionChains(GetOptionChainsRequest)} instead
   */
  @Deprecated
  public Map<String, Object> getOptionChains(String symbol, Integer expiryYear, Integer expiryMonth,
      Integer expiryDay, Integer strikePriceNear, Integer noOfStrikes, String optionCategory, String chainType) {
    return quoteClient.getOptionChains(symbol, expiryYear, expiryMonth, expiryDay, 
        strikePriceNear, noOfStrikes, optionCategory, chainType);
  }

  /**
   * Gets option expire dates for a symbol using DTOs and persists expiration dates.
   * 
   * @param request GetOptionExpireDatesRequest DTO containing symbol and expiry type filter
   * @return OptionExpireDateResponse DTO containing list of expiration dates
   */
  @Transactional
  public OptionExpireDateResponse getOptionExpireDates(GetOptionExpireDatesRequest request) {
    OptionExpireDateResponse response = marketApi.getOptionExpireDates(request);
    
    // Persist expiration dates (upsert by symbol+year+month+day)
    persistOptionExpireDates(request.getSymbol(), response);
    
    return response;
  }

  // ============================================================================
  // Persistence Helper Methods
  // ============================================================================

  /**
   * Persists lookup products (upsert by symbol+type).
   */
  private void persistLookupProducts(LookupProductResponse response) {
    try {
      List<LookupProductDto> products = response.getData();
      if (products == null || products.isEmpty()) {
        log.debug("No lookup products to persist");
        return;
      }

      for (LookupProductDto productDto : products) {
        if (productDto.getSymbol() == null || productDto.getType() == null) {
          log.warn("Lookup product missing symbol or type, skipping persistence");
          continue;
        }

        Optional<EtradeLookupProduct> existing = lookupProductRepository
            .findBySymbolAndProductType(productDto.getSymbol(), productDto.getType());
        
        EtradeLookupProduct product;
        if (existing.isPresent()) {
          // Update existing
          product = existing.get();
          product.setUpdatedAt(OffsetDateTime.now());
        } else {
          // Create new
          product = new EtradeLookupProduct();
          product.setCreatedAt(OffsetDateTime.now());
        }

        product.setSymbol(productDto.getSymbol());
        product.setProductType(productDto.getType());
        product.setDescription(productDto.getDescription());
        product.setLastSeenAt(OffsetDateTime.now());

        lookupProductRepository.save(product);
        log.debug("Persisted lookup product: {} ({})", productDto.getSymbol(), productDto.getType());
      }

      log.info("Persisted {} lookup products", products.size());
    } catch (Exception e) {
      log.error("Failed to persist lookup products", e);
      // Don't throw - persistence failure shouldn't break API call
    }
  }

  /**
   * Persists quote snapshots (append-only time-series).
   */
  private void persistQuoteSnapshots(QuoteResponse response, String detailFlag) {
    try {
      List<EtradeQuoteModel> quotes = response.getQuoteData();
      if (quotes == null || quotes.isEmpty()) {
        log.debug("No quotes to persist");
        return;
      }

      for (EtradeQuoteModel quoteModel : quotes) {
        // Use All details if available, otherwise try other detail sections
        AllQuoteDetailsDto details = quoteModel.getAll();
        if (details == null) {
          details = quoteModel.getIntraday();
        }
        if (details == null) {
          details = quoteModel.getFundamental();
        }
        if (details == null) {
          details = quoteModel.getOptions();
        }
        if (details == null) {
          details = quoteModel.getWeek52();
        }
        if (details == null) {
          details = quoteModel.getMutualFund();
        }

        if (details == null || details.getProduct() == null) {
          log.warn("Quote missing details or product, skipping persistence");
          continue;
        }

        String symbol = details.getProduct().getSymbol();
        if (symbol == null || symbol.isEmpty()) {
          log.warn("Quote missing symbol, skipping persistence");
          continue;
        }

        // Always create new snapshot (append-only)
        EtradeQuoteSnapshot snapshot = new EtradeQuoteSnapshot();
        snapshot.setSymbol(symbol);
        snapshot.setQuoteTimestamp(details.getDateTime());
        snapshot.setRequestTime(OffsetDateTime.now());
        snapshot.setDetailFlag(detailFlag);

        // Map quote fields
        snapshot.setLastTrade(toBigDecimal(details.getLastTrade()));
        snapshot.setPreviousClose(toBigDecimal(details.getPreviousClose()));
        snapshot.setOpenPrice(toBigDecimal(details.getOpen()));
        snapshot.setHigh(toBigDecimal(details.getHigh()));
        snapshot.setLow(toBigDecimal(details.getLow()));
        snapshot.setHigh52(toBigDecimal(details.getHigh52()));
        snapshot.setLow52(toBigDecimal(details.getLow52()));
        snapshot.setTotalVolume(details.getTotalVolume());
        snapshot.setVolume(details.getVolume());
        snapshot.setChangeClose(toBigDecimal(details.getChangeClose()));
        snapshot.setChangeClosePercentage(toBigDecimal(details.getChangeClosePercentage()));
        snapshot.setBid(toBigDecimal(details.getBid()));
        snapshot.setAsk(toBigDecimal(details.getAsk()));
        snapshot.setBidSize(details.getBidSize());
        snapshot.setAskSize(details.getAskSize());
        snapshot.setCompanyName(details.getCompanyName());
        snapshot.setExchange(details.getProduct().getExchange());
        snapshot.setSecurityType(details.getProduct().getSecurityType());
        snapshot.setQuoteType(details.getQuoteType());

        // Store raw response as JSON
        try {
          snapshot.setRawResponse(objectMapper.writeValueAsString(quoteModel));
        } catch (Exception e) {
          log.warn("Failed to serialize quote to JSON", e);
        }

        quoteSnapshotRepository.save(snapshot);
        log.debug("Persisted quote snapshot for symbol: {}", symbol);
      }

      log.info("Persisted {} quote snapshots", quotes.size());
    } catch (Exception e) {
      log.error("Failed to persist quote snapshots", e);
      // Don't throw - persistence failure shouldn't break API call
    }
  }

  /**
   * Persists option expiration dates (upsert by symbol+year+month+day).
   */
  private void persistOptionExpireDates(String symbol, OptionExpireDateResponse response) {
    try {
      List<OptionExpireDateDto> expireDates = response.getExpireDates();
      if (expireDates == null || expireDates.isEmpty()) {
        log.debug("No expiration dates to persist for symbol: {}", symbol);
        return;
      }

      for (OptionExpireDateDto dateDto : expireDates) {
        if (dateDto.getYear() == null || dateDto.getMonth() == null || dateDto.getDay() == null) {
          log.warn("Expiration date missing year/month/day, skipping persistence");
          continue;
        }

        Optional<EtradeOptionExpireDate> existing = optionExpireDateRepository
            .findBySymbolAndExpiryYearAndExpiryMonthAndExpiryDay(
                symbol, dateDto.getYear(), dateDto.getMonth(), dateDto.getDay());
        
        EtradeOptionExpireDate expireDate;
        if (existing.isPresent()) {
          // Update existing
          expireDate = existing.get();
          expireDate.setUpdatedAt(OffsetDateTime.now());
        } else {
          // Create new
          expireDate = new EtradeOptionExpireDate();
          expireDate.setCreatedAt(OffsetDateTime.now());
        }

        expireDate.setSymbol(symbol);
        expireDate.setExpiryYear(dateDto.getYear());
        expireDate.setExpiryMonth(dateDto.getMonth());
        expireDate.setExpiryDay(dateDto.getDay());
        expireDate.setExpiryType(dateDto.getExpiryType());
        expireDate.setLastSyncedAt(OffsetDateTime.now());

        optionExpireDateRepository.save(expireDate);
        log.debug("Persisted expiration date: {}/{}/{} for {}", 
            dateDto.getYear(), dateDto.getMonth(), dateDto.getDay(), symbol);
      }

      log.info("Persisted {} expiration dates for symbol: {}", expireDates.size(), symbol);
    } catch (Exception e) {
      log.error("Failed to persist option expiration dates", e);
      // Don't throw - persistence failure shouldn't break API call
    }
  }

  /**
   * Persists option chain snapshot (append-only).
   */
  private void persistOptionChainSnapshot(OptionChainResponse response, GetOptionChainsRequest request) {
    try {
      if (response.getSymbol() == null || response.getSymbol().isEmpty()) {
        log.warn("Option chain missing symbol, skipping snapshot persistence");
        return;
      }

      // Always create new snapshot (append-only)
      EtradeOptionChainSnapshot snapshot = new EtradeOptionChainSnapshot();
      snapshot.setSymbol(response.getSymbol());
      snapshot.setExpiryYear(request.getExpiryYear());
      snapshot.setExpiryMonth(request.getExpiryMonth());
      snapshot.setExpiryDay(request.getExpiryDay());
      snapshot.setNearPrice(toBigDecimal(response.getNearPrice()));
      snapshot.setAdjustedFlag(response.getAdjustedFlag());
      snapshot.setOptionChainType(response.getOptionChainType());
      snapshot.setQuoteType(response.getQuoteType());
      snapshot.setTimestamp(response.getTimestamp());
      snapshot.setRequestTime(OffsetDateTime.now());

      // Store raw response as JSON
      try {
        snapshot.setRawResponse(objectMapper.writeValueAsString(response));
      } catch (Exception e) {
        log.warn("Failed to serialize option chain to JSON", e);
      }

      optionChainSnapshotRepository.save(snapshot);
      log.debug("Persisted option chain snapshot for symbol: {}", response.getSymbol());
    } catch (Exception e) {
      log.error("Failed to persist option chain snapshot", e);
      // Don't throw - persistence failure shouldn't break API call
    }
  }

  /**
   * Persists option contracts (upsert by optionSymbol).
   */
  private void persistOptionContracts(OptionChainResponse response) {
    try {
      List<OptionPairDto> pairs = response.getOptionPairs();
      if (pairs == null || pairs.isEmpty()) {
        log.debug("No option pairs to persist for symbol: {}", response.getSymbol());
        return;
      }

      int persistedCount = 0;
      for (OptionPairDto pair : pairs) {
        // Persist Call option if present
        if (pair.getCall() != null) {
          persistOptionContract(response.getSymbol(), pair.getCall(), response.getSelectedED());
          persistedCount++;
        }

        // Persist Put option if present
        if (pair.getPut() != null) {
          persistOptionContract(response.getSymbol(), pair.getPut(), response.getSelectedED());
          persistedCount++;
        }
      }

      log.info("Persisted {} option contracts for symbol: {}", persistedCount, response.getSymbol());
    } catch (Exception e) {
      log.error("Failed to persist option contracts", e);
      // Don't throw - persistence failure shouldn't break API call
    }
  }

  /**
   * Persists a single option contract (upsert by optionSymbol).
   */
  private void persistOptionContract(String underlyingSymbol, OptionDto optionDto, SelectedEDDto selectedED) {
    try {
      if (optionDto.getSymbol() == null || optionDto.getSymbol().isEmpty()) {
        log.warn("Option missing symbol, skipping persistence");
        return;
      }

      Optional<EtradeOptionContract> existing = optionContractRepository
          .findByOptionSymbol(optionDto.getSymbol());
      
      EtradeOptionContract contract;
      if (existing.isPresent()) {
        // Update existing
        contract = existing.get();
        contract.setUpdatedAt(OffsetDateTime.now());
      } else {
        // Create new
        contract = new EtradeOptionContract();
        contract.setCreatedAt(OffsetDateTime.now());
      }

      contract.setOptionSymbol(optionDto.getSymbol());
      contract.setOsiKey(optionDto.getOsiKey());
      contract.setUnderlyingSymbol(underlyingSymbol);
      contract.setOptionType(optionDto.getOptionType());
      contract.setStrikePrice(toBigDecimal(optionDto.getStrikePrice()));
      
      // Use SelectedED from response if available, otherwise from optionDto
      if (selectedED != null) {
        contract.setExpiryYear(selectedED.getYear());
        contract.setExpiryMonth(selectedED.getMonth());
        contract.setExpiryDay(selectedED.getDay());
      }
      
      contract.setOptionCategory(optionDto.getOptionCategory());
      contract.setOptionRootSymbol(optionDto.getOptionRootSymbol());
      contract.setDisplaySymbol(optionDto.getDisplaySymbol());
      contract.setAdjustedFlag(optionDto.getAdjustedFlag());
      
      // Quote fields
      contract.setBid(toBigDecimal(optionDto.getBid()));
      contract.setAsk(toBigDecimal(optionDto.getAsk()));
      contract.setBidSize(optionDto.getBidSize());
      contract.setAskSize(optionDto.getAskSize());
      contract.setLastPrice(toBigDecimal(optionDto.getLastPrice()));
      contract.setVolume(optionDto.getVolume());
      contract.setOpenInterest(optionDto.getOpenInterest());
      contract.setNetChange(toBigDecimal(optionDto.getNetChange()));
      contract.setInTheMoney(optionDto.getInTheMoney());
      contract.setQuoteDetail(optionDto.getQuoteDetail());
      
      // Option Greeks
      if (optionDto.getOptionGreeks() != null) {
        OptionGreeksDto greeks = optionDto.getOptionGreeks();
        contract.setDelta(toBigDecimal(greeks.getDelta()));
        contract.setGamma(toBigDecimal(greeks.getGamma()));
        contract.setTheta(toBigDecimal(greeks.getTheta()));
        contract.setVega(toBigDecimal(greeks.getVega()));
        contract.setRho(toBigDecimal(greeks.getRho()));
        contract.setIv(toBigDecimal(greeks.getIv()));
        contract.setGreeksCurrentValue(greeks.getCurrentValue());
      }
      
      contract.setLastSyncedAt(OffsetDateTime.now());

      optionContractRepository.save(contract);
      log.debug("Persisted option contract: {}", optionDto.getSymbol());
    } catch (Exception e) {
      log.error("Failed to persist option contract: {}", optionDto.getSymbol(), e);
      // Don't throw - persistence failure shouldn't break API call
    }
  }

  /**
   * Helper method to safely convert Double to BigDecimal.
   */
  private BigDecimal toBigDecimal(Double value) {
    if (value == null) {
      return null;
    }
    return BigDecimal.valueOf(value);
  }

  /**
   * Gets option expire dates for a symbol (deprecated - uses Maps).
   * @deprecated Use {@link #getOptionExpireDates(GetOptionExpireDatesRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> getOptionExpireDates(String symbol, String expiryType) {
    return quoteClient.getOptionExpireDates(symbol, expiryType);
  }

  /**
   * Gets option expire dates for a symbol (deprecated - simplified version).
   * @deprecated Use {@link #getOptionExpireDates(GetOptionExpireDatesRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> getOptionExpireDates(String symbol) {
    return quoteClient.getOptionExpireDates(symbol);
  }
}
