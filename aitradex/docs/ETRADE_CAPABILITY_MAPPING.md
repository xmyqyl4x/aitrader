# E*TRADE API Capability Mapping

## Overview
This document maps the capabilities found in the Java and Node MVP implementations and defines the unified integration approach for aitradex.

## Reference Implementations

### Java MVP (`example-app-java`)
**Location:** `C:\dev2025\java-projects\devspaces\java_example_app\example-app-java`

**Features:**
- OAuth 1.0 3-legged authentication flow
- Account List (`/v1/accounts/list`)
- Account Balance (`/v1/accounts/{accountIdKey}/balance`)
- Portfolio (`/v1/accounts/{accountIdKey}/portfolio`)
- Quotes (`/v1/market/quote/{symbols}`)
- Order List (`/v1/accounts/{accountIdKey}/orders`)
- Order Preview (`/v1/accounts/{accountIdKey}/orders/preview`)
- Sandbox and Live environment support

**Key Classes:**
- `AuthorizationService` - Handles OAuth authorization flow
- `AccessTokenService` - Manages OAuth token exchange
- `RequestTokenService` - Generates request tokens
- `AccountListClient`, `BalanceClient`, `PortfolioClient` - Account API clients
- `QuotesClient` - Market data client
- `OrderClient` - Order management client
- `OAuth1Template` - OAuth 1.0 signing and request handling

### Node MVP (`EtradeNodeClient`)
**Location:** `C:\dev2025\java-projects\devspaces\EtradeNodeClient\EtradeNodeClient`

**Features:**
- OAuth 1.0 authentication (similar flow)
- Account management (list, balance, portfolio)
- Quote retrieval
- Order management (preview, place, cancel, view)
- Interactive terminal interface

**Key Modules:**
- `oauth/Client.js` - OAuth client implementation
- `accounts/` - Account-related operations
- `quotes/` - Quote operations
- `order/` - Order operations

## Consolidated Feature Set for aitradex

### 1. Authentication & Authorization
**Unified Approach:**
- OAuth 1.0 3-legged flow (backend-mediated)
- Request token → Authorization → Access token exchange
- Token refresh and expiration handling
- Secure token storage (encrypted)

**MVP Features:**
- ✅ Request token generation
- ✅ Authorization URL generation
- ✅ Access token exchange
- ✅ Token storage (encrypted in database)
- ✅ Session management

### 2. Account Management
**Unified Approach:**
- Account discovery and listing
- Account selection (primary/secondary)
- Account details (balance, portfolio)
- Account linkage to users

**MVP Features:**
- ✅ List accounts (`GET /v1/accounts/list`)
- ✅ Get balance (`GET /v1/accounts/{accountIdKey}/balance`)
- ✅ Get portfolio (`GET /v1/accounts/{accountIdKey}/portfolio`)
- ✅ Account metadata storage

### 3. Market Data (Quotes)
**Unified Approach:**
- Single symbol quotes
- Multi-symbol quotes
- Real-time quote retrieval
- Integration with existing market data service

**MVP Features:**
- ✅ Get quote (`GET /v1/market/quote/{symbols}`)
- ✅ Quote caching
- ✅ Error handling for invalid symbols

### 4. Order Management
**Unified Approach:**
- Order preview (validation before placement)
- Order placement
- Order cancellation
- Order status tracking
- Order history

**MVP Features:**
- ✅ Preview order (`POST /v1/accounts/{accountIdKey}/orders/preview`)
- ✅ Place order (`POST /v1/accounts/{accountIdKey}/orders/place`)
- ✅ Cancel order (`PUT /v1/accounts/{accountIdKey}/orders/cancel`)
- ✅ List orders (`GET /v1/accounts/{accountIdKey}/orders`)
- ✅ Get order details (`GET /v1/accounts/{accountIdKey}/orders/{orderId}`)

### 5. Error Handling & Resilience
**Unified Approach:**
- Rate limit handling (429 responses)
- Retry logic with exponential backoff
- Network error handling
- Authentication expiration handling
- Validation error handling

**MVP Features:**
- ✅ Retry on transient errors
- ✅ Rate limit detection and backoff
- ✅ Comprehensive error logging
- ✅ User-friendly error messages

## Overlap & Duplicate Elimination

### Duplicates Identified:
1. **OAuth Flow:** Both implement OAuth 1.0 similarly → Use Java implementation as base (matches backend stack)
2. **Account Operations:** Both have account list/balance/portfolio → Consolidate into single service
3. **Quote Retrieval:** Both fetch quotes → Integrate with existing `MarketDataService`
4. **Order Management:** Both handle orders → Use Java implementation pattern

### Differences:
- **Java MVP:** More structured, better for backend integration
- **Node MVP:** Terminal UI, but logic is similar

**Decision:** Use Java MVP as primary reference, Node MVP for functional verification only.

## Target Architecture in aitradex

### Backend (Spring Boot)
```
com.myqyl.aitradex.etrade/
├── config/
│   ├── EtradeProperties.java          # Configuration properties
│   └── EtradeOAuthConfig.java         # OAuth configuration
├── oauth/
│   ├── EtradeOAuthService.java        # OAuth flow orchestration
│   ├── EtradeOAuth1Template.java      # OAuth 1.0 signing/requests
│   ├── EtradeTokenService.java        # Token management
│   └── EtradeTokenEncryption.java     # Token encryption/decryption
├── client/
│   ├── EtradeAccountClient.java       # Account API client
│   ├── EtradeQuoteClient.java         # Quote API client
│   ├── EtradeOrderClient.java         # Order API client
│   └── EtradeApiClient.java           # Base HTTP client with OAuth
├── service/
│   ├── EtradeAccountService.java      # Account business logic
│   ├── EtradeOrderService.java        # Order business logic
│   └── EtradeQuoteService.java        # Quote business logic
├── domain/
│   ├── EtradeAccount.java             # Linked account entity
│   ├── EtradeOAuthToken.java          # OAuth token entity (encrypted)
│   ├── EtradeOrder.java               # Order entity
│   └── EtradeAuditLog.java            # Audit log entity
└── repository/
    ├── EtradeAccountRepository.java
    ├── EtradeOAuthTokenRepository.java
    ├── EtradeOrderRepository.java
    └── EtradeAuditLogRepository.java
```

### API Controllers
```
com.myqyl.aitradex.api.controller/
├── EtradeOAuthController.java         # OAuth initiation/callback
├── EtradeAccountController.java       # Account operations
├── EtradeOrderController.java         # Order operations
└── EtradeQuoteController.java         # Quote operations (extends existing)
```

### Frontend (Angular)
```
src/app/features/etrade-review-trade/
├── etrade-review-trade.component.ts
├── etrade-review-trade.component.html
├── etrade-review-trade.component.scss
├── components/
│   ├── account-list/
│   ├── account-balance/
│   ├── order-form/
│   ├── order-list/
│   └── quote-display/
└── services/
    └── etrade.service.ts
```

### Database Schema
```sql
-- Linked E*TRADE accounts
etrade_account (
  id UUID PRIMARY KEY,
  user_id UUID,
  account_id_key VARCHAR(255) UNIQUE,
  account_type VARCHAR(50),
  account_name VARCHAR(255),
  account_status VARCHAR(50),
  linked_at TIMESTAMPTZ,
  last_synced_at TIMESTAMPTZ
)

-- Encrypted OAuth tokens
etrade_oauth_token (
  id UUID PRIMARY KEY,
  account_id UUID REFERENCES etrade_account(id),
  access_token_encrypted TEXT,
  access_token_secret_encrypted TEXT,
  token_type VARCHAR(50),
  expires_at TIMESTAMPTZ,
  refresh_token_encrypted TEXT,
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ
)

-- Orders
etrade_order (
  id UUID PRIMARY KEY,
  account_id UUID REFERENCES etrade_account(id),
  etrade_order_id VARCHAR(255),
  symbol VARCHAR(10),
  order_type VARCHAR(50),
  price_type VARCHAR(50),
  quantity INTEGER,
  limit_price NUMERIC(19,8),
  stop_price NUMERIC(19,8),
  order_status VARCHAR(50),
  placed_at TIMESTAMPTZ,
  executed_at TIMESTAMPTZ,
  cancelled_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ
)

-- Audit log
etrade_audit_log (
  id UUID PRIMARY KEY,
  account_id UUID REFERENCES etrade_account(id),
  action VARCHAR(100),
  resource_type VARCHAR(50),
  resource_id VARCHAR(255),
  request_body JSONB,
  response_body JSONB,
  status_code INTEGER,
  error_message TEXT,
  created_at TIMESTAMPTZ
)
```

## Implementation Priority

### Phase 1: Core Infrastructure
1. Configuration and properties
2. OAuth 1.0 service
3. Token encryption/decryption
4. Base API client with OAuth signing

### Phase 2: Account Management
1. Account list client/service
2. Balance client/service
3. Portfolio client/service
4. Account linking UI

### Phase 3: Market Data
1. Quote client
2. Integration with MarketDataService
3. Quote display in UI

### Phase 4: Order Management
1. Order preview client/service
2. Order placement client/service
3. Order list/cancel client/service
4. Order form UI

### Phase 5: Testing & Polish
1. Unit tests
2. Integration tests with mocks
3. E2E tests
4. Error handling improvements
5. Documentation

## Security Considerations

1. **Secrets Management:**
   - API keys/secrets via environment variables
   - Never log secrets
   - Encryption for stored tokens

2. **OAuth Token Storage:**
   - Encrypt access tokens and secrets at rest
   - Use AES-256 encryption
   - Store encryption key securely (not in code)

3. **Callback URL:**
   - Validated callback URL
   - CSRF protection
   - State parameter validation

4. **Rate Limiting:**
   - Respect E*TRADE API rate limits
   - Implement client-side rate limiting
   - Graceful degradation on rate limit errors

## Testing Strategy

### Backend Tests
- Unit tests for OAuth signing
- Unit tests for API clients (mocked responses)
- Integration tests with mock E*TRADE server
- Token encryption/decryption tests

### Frontend Tests
- Unit tests for components
- Service tests with mocked HTTP
- E2E tests for full flows (auth → account → order)

### Mock E*TRADE API
- Use WireMock or similar for integration tests
- Mock all E*TRADE endpoints
- Test error scenarios (401, 429, 500)
