# E*TRADE Orders API Documentation

## Overview

This document describes the E*TRADE Orders API integration in the `aitradex` application, including API endpoints, persistence behavior, and testing approach.

## Prerequisite: Valid OAuth Access Token

All Orders API calls require a **valid OAuth 1.0a access token + token secret**. The access token:
- Expires by default at end of day (US Eastern time)
- Can become inactive after ~2 hours of inactivity and must be renewed
- Must be included in the `Authorization: OAuth ...` header for all requests

## Orders API Flow

### Step 0: Get `accountIdKey`

All Orders endpoints are scoped to a brokerage account via `{accountIdKey}`, which is retrieved from the "List Accounts" API endpoint.

### Step 1: List Orders

**Endpoint**: `GET /v1/accounts/{accountIdKey}/orders`

**Query Parameters**:
- `count` (default 25, max 100) - Number of orders to return
- `marker` - Pagination marker for next page
- `status` - Filter by status (OPEN, EXECUTED, CANCELLED, etc.)
- `fromDate` / `toDate` - Date range filter (MMDDYYYY format, up to 2 years)
- `symbol` - Symbol filter (up to 25 comma-separated symbols)
- `securityType` - Security type filter (EQ, OPTN, MF, MMF)
- `transactionType` - Transaction type filter (ATNM, BUY, SELL, etc.)
- `marketSession` - Market session filter (REGULAR, EXTENDED)

**Response**: `OrdersResponse` containing:
- `orders` - Array of `Order` entries
- `marker` - Pagination marker for next page
- `moreOrders` - Boolean indicating if more orders are available

**Database Persistence**:
- **Upsert orders** by `orderId` + `accountIdKey` (unique constraint)
- Update `lastSyncedAt` timestamp on each sync
- Store order status, placedTime, and other fields from response
- Store raw order response as JSONB for audit/debugging

**Application Endpoint**: `GET /api/etrade/orders/list?accountId={accountId}`

### Step 2: Preview Order

**Endpoint**: `POST /v1/accounts/{accountIdKey}/orders/preview`

**Request**: `PreviewOrderRequest` containing:
- `orderType` - Order type (EQ, OPTN, SPREADS, etc.)
- `clientOrderId` - Client-provided order ID (optional)
- `Order[]` - Array of order details:
  - `priceType` - Price type (MARKET, LIMIT, STOP, etc.)
  - `orderTerm` - Order term (GOOD_FOR_DAY, GOOD_UNTIL_CANCEL, etc.)
  - `marketSession` - Market session (REGULAR, EXTENDED)
  - `limitPrice` / `stopPrice` - Price limits (optional)
  - `Instrument[]` - Array of instruments:
    - `Product` - Product details (symbol, securityType, etc.)
    - `orderAction` - Order action (BUY, SELL)
    - `quantity` - Order quantity
    - `quantityType` - Quantity type (QUANTITY, DOLLAR, etc.)

**Response**: `PreviewOrderResponse` containing:
- `previewIds` - Array of preview IDs (use first one for place order)
- `previewTime` - Preview timestamp
- `totalOrderValue` - Estimated total order value
- `estimatedCommission` - Estimated commission
- `estimatedTotalAmount` - Estimated total amount
- `messages` - Array of warnings/messages
- `Order[]` - Order details with estimates

**Database Persistence**:
- **Save preview attempt** as a new order record with:
  - `previewId` - The preview ID from response
  - `clientOrderId` - Client order ID from request
  - `previewTime` - Timestamp from response
  - `previewData` - Full preview response stored as JSONB
  - `orderStatus` - Set to "PREVIEW"
- Preview records are separate from placed orders (no unique constraint on previewId)

**Application Endpoint**: `POST /api/etrade/orders/preview?accountId={accountId}`

### Step 3: Place Order

**Endpoint**: `POST /v1/accounts/{accountIdKey}/orders/place`

**Request**: `PlaceOrderRequest` containing:
- `PreviewId` - Preview ID from Step 2
- `orderType` - Order type (must match preview)
- `clientOrderId` - Client order ID (optional)
- `Order[]` - Order details (same structure as preview)

**Response**: `PlaceOrderResponse` containing:
- `OrderIds` - Array of order IDs
- `placedTime` - Epoch milliseconds timestamp
- `messages` - Array of messages/warnings

**Database Persistence**:
- **Upsert order** by `orderId` + `accountIdKey`:
  - `etradeOrderId` - Order ID from response
  - `placedTime` - Epoch milliseconds from response
  - `placedAt` - Converted OffsetDateTime from placedTime
  - `previewId` - Preview ID used
  - `clientOrderId` - Client order ID
  - `orderStatus` - Set to "SUBMITTED"
  - `previewData` - Preview response stored as JSONB
  - `orderResponse` - Place order response stored as JSONB
- Extract order details (symbol, quantity, side, priceType, etc.) from request

**Application Endpoint**: `POST /api/etrade/orders?accountId={accountId}`

### Step 4: Cancel Order

**Endpoint**: `PUT /v1/accounts/{accountIdKey}/orders/cancel`

**Request**: `CancelOrderRequest` containing:
- `orderId` - E*TRADE order ID to cancel

**Response**: `CancelOrderResponse` containing:
- `success` - Boolean indicating success
- `messages` - Array of messages

**Database Persistence**:
- **Update existing order**:
  - `orderStatus` - Set to "CANCELLED"
  - `cancelledAt` - Set to current timestamp
  - `cancelTime` - Epoch milliseconds (if provided in response)

**Application Endpoint**: `DELETE /api/etrade/orders/{orderId}?accountId={accountId}`

### Step 5: Change Previewed Order

**Endpoint**: `PUT /v1/accounts/{accountIdKey}/orders/{orderId}/change/preview`

**Request**: `PreviewOrderRequest` (same structure as Step 2, but for modifying an existing order)

**Response**: `PreviewOrderResponse` (same structure as Step 2)

**Database Persistence**:
- **Save change-preview attempt**:
  - Original `orderId` is preserved
  - New `previewId` is stored
  - `previewData` is updated with new preview response
  - `previewTime` is updated

**Application Endpoint**: `PUT /api/etrade/orders/{orderId}/preview?accountId={accountId}`

### Step 6: Place Changed Order

**Endpoint**: `PUT /v1/accounts/{accountIdKey}/orders/{orderId}/change/place`

**Request**: `PlaceOrderRequest` (same structure as Step 3, but for modified order)

**Response**: `PlaceOrderResponse` (same structure as Step 3)

**Database Persistence**:
- **Update existing order**:
  - `etradeOrderId` - May be updated if order ID changes
  - `placedTime` - Updated from response
  - `placedAt` - Updated from placedTime
  - `previewData` - Updated with latest preview
  - `orderResponse` - Updated with latest place response
  - `orderStatus` - Set to "SUBMITTED"

**Application Endpoint**: `PUT /api/etrade/orders/{orderId}?accountId={accountId}`

## Database Schema

### `etrade_order` Table

**Key Fields**:
- `id` - Internal UUID (primary key)
- `account_id` - Internal account UUID (foreign key to `etrade_account`)
- `account_id_key` - E*TRADE account ID key
- `etrade_order_id` - E*TRADE order ID
- `client_order_id` - Client-provided order ID
- `preview_id` - Preview ID (for preview attempts)
- `symbol` - Security symbol
- `order_type` - Order type (EQ, OPTN, etc.)
- `price_type` - Price type (MARKET, LIMIT, etc.)
- `side` - Order side (BUY, SELL)
- `quantity` - Order quantity
- `limit_price` / `stop_price` - Price limits
- `order_status` - Order status (PREVIEW, SUBMITTED, OPEN, EXECUTED, CANCELLED, etc.)
- `placed_time` - Epoch milliseconds from E*TRADE
- `placed_at` - OffsetDateTime (converted from placedTime)
- `cancelled_at` - Cancellation timestamp
- `cancel_time` - Epoch milliseconds from E*TRADE (if provided)
- `preview_time` - Preview timestamp
- `last_synced_at` - Last sync timestamp (for List Orders)
- `preview_data` - Preview response stored as JSONB
- `order_response` - Order response stored as JSONB
- `created_at` / `updated_at` - Audit timestamps

**Unique Constraint**: `(etrade_order_id, account_id_key)` - Ensures upsert logic works correctly

**Indexes**:
- `idx_etrade_order_account_id` - For account-based queries
- `idx_etrade_order_etrade_order_id` - For order ID lookups
- `idx_etrade_order_symbol` - For symbol-based queries
- `idx_etrade_order_status` - For status-based queries
- `idx_etrade_order_placed_at` - For time-based queries (descending)
- `idx_etrade_order_last_synced_at` - For sync tracking (descending)

## Testing Approach

### Functional Tests

All Orders API tests are **functional tests** that:
- Call our application's REST API endpoints (not E*TRADE directly)
- Make real calls to E*TRADE sandbox (not mocked)
- Validate database persistence after each API call
- Use `assumeTrue` to skip tests when credentials are not available

### Test Coverage

1. **Token Prerequisite Enforcement** (`test1_tokenPrerequisiteEnforcement_ordersApiRequiresOAuthToken`)
   - Validates that Orders API requires OAuth token
   - Tests error handling for non-existent accounts

2. **List Orders Happy Path** (`test2_listOrders_happyPath`)
   - Calls List Orders endpoint
   - Validates response structure
   - Validates database persistence (upsert by orderId + accountIdKey)

3. **Preview → Place → Cancel End-to-End** (`test3_previewPlaceCancel_endToEnd`)
   - Tests complete order lifecycle:
     - Preview order → validate previewId persistence
     - Place order → validate orderId and placedTime persistence
     - Cancel order → validate cancellation status and cancelledAt persistence

4. **Invalid AccountIdKey** (`test4_listOrders_invalidAccountIdKey`)
   - Tests error handling for invalid accountIdKey

5. **Pagination and Filters** (`test5_listOrders_paginationAndFilters`)
   - Tests List Orders with pagination parameters and filters

### Running Tests

**Prerequisites**:
- Local PostgreSQL database running on localhost:5432
- Database `aitradexdb` exists
- User `aitradex_user` with password `aitradex_pass` has access
- Environment variables set:
  - `ETRADE_CONSUMER_KEY`
  - `ETRADE_CONSUMER_SECRET`
  - `ETRADE_ENCRYPTION_KEY`
  - `ETRADE_OAUTH_VERIFIER` (optional - for automatic token exchange)
  - `ETRADE_ACCOUNT_ID_KEY` (optional - will be retrieved from List Accounts if not set)

**Run Tests**:
```bash
cd aitradex-service
mvn test -Dtest=EtradeOrdersFunctionalTest
```

**Expected Results**:
- Tests that require credentials will be skipped if credentials are not available
- Tests that run will validate API calls and database persistence
- All tests should pass when credentials are properly configured

## Implementation Notes

### Service Layer

- `EtradeOrderService.listOrders()` - Calls API and persists orders (upsert)
- `EtradeOrderService.previewOrder()` - Calls API and persists preview attempt
- `EtradeOrderService.placeOrder()` - Calls preview, then place, and persists order
- `EtradeOrderService.cancelOrder()` - Calls API and updates order status
- `EtradeOrderService.changePreviewOrder()` - Calls API and persists change preview
- `EtradeOrderService.placeChangedOrder()` - Calls preview, then place changed, and updates order

### Transaction Management

- All service methods use `@Transactional(noRollbackFor = EtradeApiException.class)` to prevent rollback on API exceptions
- This allows the exception handler to return proper HTTP error responses

### Error Handling

- `EtradeApiException` is thrown for API errors
- `ApiExceptionHandler` catches `EtradeApiException` and returns structured error responses
- Database persistence failures are logged but don't break API calls

## Removed Mock Tests

The following mock-based tests have been removed and replaced with functional tests:
- `EtradeOrdersApiIntegrationTest` - Mock-based integration tests
- `EtradeApiClientOrderAPITest` - Mock-based unit tests
- `EtradeOrderClientTest` - Mock-based unit tests
- `EtradeOrdersApiTest` - Standalone tests calling E*TRADE directly

All Orders API testing is now done through functional tests that call our application's REST API endpoints.
