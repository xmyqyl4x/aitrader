package com.myqyl.aitradex.marketdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.config.AlphaVantageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Alpha Vantage API.
 * Only runs when ALPHA_VANTAGE_API_KEY environment variable is set.
 * This validates that the Alpha Vantage service is working correctly.
 */
@EnabledIfEnvironmentVariable(named = "ALPHA_VANTAGE_API_KEY", matches = ".+")
class AlphaVantageAdapterIntegrationTest {

  private AlphaVantageAdapter adapter;
  private AlphaVantageProperties properties;

  @BeforeEach
  void setUp() {
    properties = new AlphaVantageProperties();
    String apiKey = System.getenv("ALPHA_VANTAGE_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      apiKey = "B8IQ3ONTC1RTKRH7"; // Default from requirements
    }
    properties.setApiKey(apiKey);
    properties.setBaseUrl("https://www.alphavantage.co");
    properties.setEnabled(true);
    
    adapter = new AlphaVantageAdapter(new ObjectMapper(), properties);
  }

  @Test
  void latestQuote_fetchesRealQuoteForApple() {
    // This test makes a real API call to Alpha Vantage
    MarketDataQuoteDto quote = adapter.latestQuote("AAPL");

    assertNotNull(quote, "Quote should not be null");
    assertEquals("AAPL", quote.symbol());
    assertNotNull(quote.asOf(), "As-of date should not be null");
    assertNotNull(quote.close(), "Close price should not be null");
    assertEquals("alphavantage", quote.source());
    
    // Log the quote for verification
    System.out.println("✅ Alpha Vantage Integration Test PASSED");
    System.out.println("   Symbol: " + quote.symbol());
    System.out.println("   Close Price: $" + quote.close());
    System.out.println("   As Of: " + quote.asOf());
    System.out.println("   Volume: " + quote.volume());
  }

  @Test
  void latestQuote_fetchesRealQuoteForMicrosoft() {
    // Test with another symbol to ensure consistency
    MarketDataQuoteDto quote = adapter.latestQuote("MSFT");

    assertNotNull(quote);
    assertEquals("MSFT", quote.symbol());
    assertNotNull(quote.close());
    
    System.out.println("✅ Alpha Vantage MSFT Quote: $" + quote.close());
  }

  @Test
  void intradayQuote_fetchesRealIntradayData() {
    // Test intraday functionality
    MarketDataQuoteDto quote = adapter.intradayQuote("AAPL");

    assertNotNull(quote);
    assertEquals("AAPL", quote.symbol());
    assertNotNull(quote.close());
    
    System.out.println("✅ Alpha Vantage Intraday Quote: $" + quote.close() + " @ " + quote.asOf());
  }
}
