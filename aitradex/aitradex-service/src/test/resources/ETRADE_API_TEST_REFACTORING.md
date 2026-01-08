# E*TRADE API Test Refactoring

## Overview

All E*TRADE API tests have been refactored to validate **our application's E*TRADE integration layer** instead of calling E*TRADE's public endpoints directly.

## Architecture

### Test Structure

```
aitradex-service/src/test/java/com/myqyl/aitradex/etrade/api/
├── integration/                          # NEW: Integration tests (use our app endpoints)
│   ├── EtradeApiIntegrationTestBase.java # Base class with Spring Boot + MockMvc
│   ├── EtradeOrdersApiIntegrationTest.java
│   ├── EtradeQuotesApiIntegrationTest.java
│   ├── EtradeTransactionsApiIntegrationTest.java
│   └── EtradeAccountsApiIntegrationTest.java
└── [old standalone tests]                # OLD: Direct E*TRADE API calls (deprecated)
    ├── EtradeOrdersApiTest.java
    ├── EtradeQuotesApiTest.java
    ├── EtradeTransactionsApiTest.java
    ├── EtradeAccountsApiTest.java
    ├── EtradeAccountBalanceApiTest.java
    └── EtradePortfolioApiTest.java
```

### How Integration Tests Work

1. **Spring Boot Test Context**: Tests use `@SpringBootTest` with Testcontainers PostgreSQL
2. **MockMvc**: Tests call our REST API endpoints (`/api/etrade/*`) via MockMvc
3. **Mocked E*TRADE Clients**: E*TRADE client layer (`EtradeOrderClient`, `EtradeQuoteClient`, `EtradeAccountClient`) is mocked using `@MockBean`
4. **Validation**: Tests validate:
   - Our request building logic
   - Our response parsing logic
   - Our error handling
   - Our service/controller behavior

### What Tests Validate

✅ **Our Application Logic**:
- Request parameter handling
- Response mapping/DTOs
- Error handling and exceptions
- Database persistence (where applicable)
- Service layer orchestration

❌ **NOT Validated** (by design):
- E*TRADE's API behavior (we mock their responses)
- Network connectivity to E*TRADE
- OAuth token validity (tokens are mocked)

## Test Coverage

### Orders API (`EtradeOrdersApiIntegrationTest`)
- ✅ List Orders
- ✅ Preview Order (Market, Limit)
- ✅ Place Order
- ✅ Cancel Order
- ✅ Error handling (invalid account, invalid order)

### Quotes API (`EtradeQuotesApiIntegrationTest`)
- ✅ Get Quote (single symbol)
- ✅ Get Quotes (multiple symbols)
- ✅ Look Up Product (by symbol, by company name)
- ✅ Get Option Chains (basic, with filters)
- ✅ Get Option Expire Dates
- ✅ Error handling (invalid symbol, invalid account)

### Transactions API (`EtradeTransactionsApiIntegrationTest`)
- ✅ List Transactions (basic, with pagination, with marker)
- ✅ Get Transaction Details
- ✅ Error handling (invalid account, invalid transaction ID)

### Accounts API (`EtradeAccountsApiIntegrationTest`)
- ✅ Get User Accounts
- ✅ Get Account
- ✅ Get Account Balance
- ✅ Get Account Portfolio
- ✅ Sync Accounts
- ✅ Unlink Account
- ✅ Error handling (invalid account)

## Running Tests

### Integration Tests (Recommended)

```bash
# Run all integration tests
mvn test -Dtest=*IntegrationTest

# Run specific test class
mvn test -Dtest=EtradeOrdersApiIntegrationTest
```

### Old Standalone Tests (Deprecated)

The old standalone tests (`EtradeOrdersApiTest`, `EtradeQuotesApiTest`, etc.) that directly call E*TRADE's API are **deprecated** but remain in the codebase for reference. They require:

- `ETRADE_CONSUMER_KEY`
- `ETRADE_CONSUMER_SECRET`
- `ETRADE_ACCESS_TOKEN` (or `ETRADE_OAUTH_VERIFIER`)
- `ETRADE_ACCESS_TOKEN_SECRET`

These tests are **not recommended** for regular use as they:
- Call E*TRADE's public endpoints directly
- Require valid OAuth tokens
- Are slower and less reliable
- Don't validate our application logic

## Migration Notes

### Before (Old Standalone Tests)
```java
// Directly calls E*TRADE API
StandaloneEtradeApiClient apiClient = new StandaloneEtradeApiClient(...);
String response = apiClient.get("/v1/market/quote/AAPL", params);
```

### After (New Integration Tests)
```java
// Calls our application endpoint
mockMvc.perform(get("/api/etrade/quotes/{symbol}", "AAPL")
        .param("accountId", testAccountId.toString()))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.symbol").value("AAPL"));

// E*TRADE client is mocked
when(quoteClient.getQuotes(eq(testAccountId), eq("AAPL")))
    .thenReturn(List.of(mockQuote));
```

## Benefits

1. **Faster**: No network calls to E*TRADE
2. **Reliable**: No dependency on E*TRADE API availability
3. **Focused**: Tests validate our code, not E*TRADE's API
4. **Deterministic**: Mock responses ensure consistent test results
5. **Comprehensive**: Tests cover all our application endpoints

## Future Work

- [ ] Remove old standalone tests (or move to separate package)
- [ ] Add more edge case tests
- [ ] Add performance tests for our application layer
- [ ] Add contract tests if needed (separate from integration tests)
