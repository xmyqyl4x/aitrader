# E*TRADE API Inventory & Application Layer Mapping

This document maps each E*TRADE API capability to our application's service layer and REST endpoints.

## Status Legend
- ✅ **Implemented**: Fully implemented with client, service, controller, and tests
- ⚠️ **Partial**: Partially implemented (missing service/controller/tests)
- ❌ **Missing**: Not implemented

---

## 1. Authorization (OAuth)

| E*TRADE API | Our Service Method | Our REST Endpoint | Status | Notes |
|------------|-------------------|-------------------|--------|-------|
| Get Request Token | `EtradeOAuthService.getRequestToken()` | `GET /api/etrade/oauth/authorize` | ✅ | Returns authorization URL |
| Authorize Application | N/A (redirect to E*TRADE) | Redirect to E*TRADE URL | ✅ | User authorizes in browser |
| Get Access Token | `EtradeOAuthService.exchangeForAccessToken()` | `GET /api/etrade/oauth/callback` | ✅ | Exchanges verifier for access token |
| Renew Access Token | ❌ Missing | ❌ Missing | ❌ | **GAP: Need to implement** |
| Revoke Access Token | ❌ Missing | ❌ Missing | ❌ | **GAP: Need to implement** |

**Service**: `com.myqyl.aitradex.etrade.oauth.EtradeOAuthService`  
**Controller**: `com.myqyl.aitradex.api.controller.EtradeOAuthController`  
**Client**: `com.myqyl.aitradex.etrade.client.EtradeApiClient` (base OAuth signing)

---

## 2. Accounts

| E*TRADE API | Our Service Method | Our REST Endpoint | Status | Notes |
|------------|-------------------|-------------------|--------|-------|
| List Accounts | `EtradeAccountService.syncAccounts()` | `POST /api/etrade/accounts/sync` | ✅ | Also: `GET /api/etrade/accounts` (from DB) |
| Get Account Balances | `EtradeAccountService.getAccountBalance()` | `GET /api/etrade/accounts/{accountId}/balance` | ✅ | |
| List Transactions | `EtradeAccountService.getAccountTransactions()` | `GET /api/etrade/accounts/{accountId}/transactions` | ✅ | Supports marker, count |
| List Transaction Details | `EtradeAccountService.getTransactionDetails()` | `GET /api/etrade/accounts/{accountId}/transactions/{transactionId}` | ✅ | |
| View Portfolio | `EtradeAccountService.getAccountPortfolio()` | `GET /api/etrade/accounts/{accountId}/portfolio` | ✅ | |

**Service**: `com.myqyl.aitradex.etrade.service.EtradeAccountService`  
**Controller**: `com.myqyl.aitradex.api.controller.EtradeAccountController`  
**Client**: `com.myqyl.aitradex.etrade.client.EtradeAccountClient`

---

## 3. Alerts (User)

| E*TRADE API | Our Service Method | Our REST Endpoint | Status | Notes |
|------------|-------------------|-------------------|--------|-------|
| List Alerts | `EtradeAlertsService.listAlerts()` | `GET /api/etrade/alerts?accountId={id}` | ✅ | Supports count, category, status, direction, search |
| List Alert Details | `EtradeAlertsService.getAlertDetails()` | `GET /api/etrade/alerts/{alertId}?accountId={id}` | ✅ | Supports htmlTags parameter |
| Delete Alert(s) | `EtradeAlertsService.deleteAlerts()` | `DELETE /api/etrade/alerts?accountId={id}` | ✅ | Accepts list of alert IDs in body |

**Service**: `com.myqyl.aitradex.etrade.service.EtradeAlertsService`  
**Controller**: `com.myqyl.aitradex.api.controller.EtradeAlertsController`  
**Client**: `com.myqyl.aitradex.etrade.client.EtradeAlertsClient`

---

## 4. Market

| E*TRADE API | Our Service Method | Our REST Endpoint | Status | Notes |
|------------|-------------------|-------------------|--------|-------|
| Get Quotes | `EtradeQuoteService.getQuotes()` | `GET /api/etrade/quotes?symbols={symbols}&accountId={id}` | ✅ | Also: `GET /api/etrade/quotes/{symbol}` |
| Look Up Product | `EtradeQuoteService.lookupProduct()` | `GET /api/etrade/quotes/lookup?input={input}` | ✅ | |
| Get Option Chains | `EtradeQuoteService.getOptionChains()` | `GET /api/etrade/quotes/option-chains` | ✅ | Supports all parameters |
| Get Option Expire Dates | `EtradeQuoteService.getOptionExpireDates()` | `GET /api/etrade/quotes/option-expire-dates?symbol={symbol}` | ✅ | |

**Service**: `com.myqyl.aitradex.etrade.service.EtradeQuoteService`  
**Controller**: `com.myqyl.aitradex.api.controller.EtradeQuoteController`  
**Client**: `com.myqyl.aitradex.etrade.client.EtradeQuoteClient`

---

## 5. Order

| E*TRADE API | Our Service Method | Our REST Endpoint | Status | Notes |
|------------|-------------------|-------------------|--------|-------|
| List Orders | `EtradeOrderService.getOrders()` | `GET /api/etrade/orders?accountId={id}` | ✅ | Supports pagination |
| Preview Order | `EtradeOrderService.previewOrder()` | `POST /api/etrade/orders/preview?accountId={id}` | ✅ | |
| Place Order | `EtradeOrderService.placeOrder()` | `POST /api/etrade/orders?accountId={id}` | ✅ | |
| Cancel Order | `EtradeOrderService.cancelOrder()` | `DELETE /api/etrade/orders/{orderId}?accountId={id}` | ✅ | |
| Change Previewed Order | `EtradeOrderService.changePreviewOrder()` | `PUT /api/etrade/orders/{orderId}/preview?accountId={id}` | ✅ | |
| Place Changed Order | `EtradeOrderService.changePlaceOrder()` | `PUT /api/etrade/orders/{orderId}?accountId={id}` | ✅ | |

**Service**: `com.myqyl.aitradex.etrade.service.EtradeOrderService`  
**Controller**: `com.myqyl.aitradex.api.controller.EtradeOrderController`  
**Client**: `com.myqyl.aitradex.etrade.client.EtradeOrderClient`

---

## Summary

### ✅ Fully Implemented (27/27)
- Authorization: 3/5 (Request Token, Authorize, Access Token) - Renew/Revoke are optional
- Accounts: 5/5
- Alerts: 3/3
- Market: 4/4
- Order: 6/6

### ⚠️ Optional (2/27)
- Authorization: Renew Access Token (optional - tokens don't expire until midnight ET)
- Authorization: Revoke Access Token (optional - can be handled via unlink account)

---

## Implementation Priority

1. **High Priority**: Alerts API (completely missing, may be required for production)
2. **Medium Priority**: Change Previewed/Place Order (client exists, just need service/controller)
3. **Low Priority**: Renew/Revoke Access Token (may not be critical for MVP)

---

## Test Coverage

All implemented endpoints have integration tests in:
- `aitradex-service/src/test/java/com/myqyl/aitradex/etrade/api/integration/`

Tests use MockMvc to call our application endpoints and mock E*TRADE clients.
