# E*TRADE API Contract Gap Analysis

This document identifies gaps between our implementation and E*TRADE's documented API contracts (request parameters and response fields).

## Analysis Methodology

For each E*TRADE API endpoint:
1. **Request Parameters**: Check if all documented query parameters and request body fields are supported
2. **Response Fields**: Check if all documented response fields are parsed and returned
3. **Data Structures**: Verify nested objects, arrays, and optional fields are handled correctly

---

## 1. Authorization (OAuth)

### ✅ Get Request Token
- **Status**: Complete
- **Request**: `oauth_callback` parameter supported
- **Response**: All fields parsed (`oauth_token`, `oauth_token_secret`, `oauth_callback_confirmed`)

### ✅ Get Access Token
- **Status**: Complete
- **Request**: `oauth_verifier` parameter supported
- **Response**: All fields parsed (`oauth_token`, `oauth_token_secret`)

### ⚠️ Renew Access Token
- **Status**: Not implemented (optional - tokens expire at midnight ET)

### ⚠️ Revoke Access Token
- **Status**: Not implemented (optional - can be handled via account unlinking)

---

## 2. Accounts API

### ⚠️ List Accounts (`/v1/accounts/list`)
**Status**: **INCOMPLETE - Missing Response Fields**

**Missing Response Fields:**
- ❌ `accountMode` - Account mode (e.g., "CASH", "MARGIN")
- ❌ `institutionType` - Institution type

**Current Implementation:**
```java
// Only extracts: accountIdKey, accountId, accountName, accountType, accountDesc, accountStatus
```

**Required Fix:**
- Add `accountMode` and `institutionType` to `parseAccount()` method

---

### ⚠️ Get Account Balances (`/v1/accounts/{accountIdKey}/balance`)
**Status**: **INCOMPLETE - Missing Parameters & Response Fields**

**Missing Request Parameters:**
- ❌ `accountType` - Filter by account type (optional)
- ✅ `instType` - Supported (hardcoded to "BROKERAGE")
- ✅ `realTimeNAV` - Supported (hardcoded to "true")

**Missing Response Fields:**
- ❌ `Cash` section - Cash balance details
- ❌ `Margin` section - Margin balance details
- ⚠️ `Computed` section - Partially parsed (missing some fields)

**Current Implementation:**
```java
// Only extracts: accountId, accountType, computed.total, computed.netCash, computed.cashAvailableForInvestment
```

**Required Fix:**
- Add `accountType` as optional parameter
- Parse `Cash` section (cashBalance, cashAvailable, etc.)
- Parse `Margin` section (marginBalance, marginAvailable, etc.)
- Parse all `Computed` fields (totalValue, netValue, etc.)

---

### ⚠️ View Portfolio (`/v1/accounts/{accountIdKey}/portfolio`)
**Status**: **INCOMPLETE - Missing Parameters & Response Fields**

**Missing Request Parameters:**
- ❌ `count` - Number of positions to return
- ❌ `sortBy` - Sort field (e.g., "SYMBOL", "QUANTITY", "MARKET_VALUE")
- ❌ `sortOrder` - Sort direction ("ASC", "DESC")
- ❌ `pageNumber` - Page number for pagination
- ❌ `marketSession` - Market session filter
- ❌ `totalsRequired` - Whether to include totals
- ❌ `lotsRequired` - Whether to include lot details
- ❌ `view` - View type (e.g., "QUICK", "COMPLETE")

**Missing Response Fields:**
- ❌ `totalPages` - Total number of pages
- ❌ `Position` fields: `positionId`, `positionType`, `marketValue`, `costBasis`, `gainLoss`, `gainLossPercent`
- ❌ `Product` fields: `symbolDescription`, `cusip`, `isQuotable`
- ⚠️ Some position fields may be missing

**Current Implementation:**
```java
// Only extracts: accountId, positions (with symbol, quantity, lastTrade, pricePaid, costBasis)
// Missing: totalPages, positionId, positionType, marketValue, gainLoss, gainLossPercent
```

**Required Fix:**
- Add all query parameters to `getPortfolio()` method
- Parse `totalPages` from response
- Parse all `Position` fields (positionId, positionType, marketValue, gainLoss, etc.)
- Parse all `Product` fields (symbolDescription, cusip, isQuotable)

---

### ⚠️ List Transactions (`/v1/accounts/{accountIdKey}/transactions`)
**Status**: **INCOMPLETE - Missing Parameters & Response Fields**

**Missing Request Parameters:**
- ❌ `startDate` - Start date filter (MMddyyyy format)
- ❌ `endDate` - End date filter (MMddyyyy format)
- ❌ `sortOrder` - Sort direction ("ASC", "DESC")
- ❌ `accept` - Response format ("xml" or "json")
- ❌ `storeId` - Store ID filter

**Missing Response Fields:**
- ❌ `transactionCount` - Number of transactions returned
- ❌ `totalCount` - Total number of transactions
- ❌ `moreTransactions` - Whether more transactions exist
- ❌ `next` - Next page marker
- ❌ `marker` - Current page marker

**Current Implementation:**
```java
// Only extracts: transactionId, accountId, transactionDate, amount, description, transactionType, instType, detailsURI
// Missing: transactionCount, totalCount, moreTransactions, next, marker
```

**Required Fix:**
- Add `startDate`, `endDate`, `sortOrder`, `accept`, `storeId` parameters
- Parse response metadata: `transactionCount`, `totalCount`, `moreTransactions`, `next`, `marker`

---

### ⚠️ Get Transaction Details (`/v1/accounts/{accountIdKey}/transactions/{tranid}`)
**Status**: **INCOMPLETE - Missing Parameters & Response Fields**

**Missing Request Parameters:**
- ❌ `accept` - Response format ("xml" or "json")
- ❌ `storeId` - Store ID filter

**Missing Response Fields:**
- ⚠️ Need to verify all `Category` fields are parsed
- ⚠️ Need to verify all `Brokerage` fields are parsed
- ⚠️ Need to verify all `Transaction` fields are parsed

**Current Implementation:**
```java
// Parses: transactionId, accountId, transactionDate, amount, description, category, brokerage
// Need to verify completeness
```

**Required Fix:**
- Add `accept` and `storeId` parameters
- Verify all nested fields in `Category`, `Brokerage`, and `Transaction` are parsed

---

## 3. Alerts API

### ⚠️ List Alerts (`/v1/user/alerts`)
**Status**: **INCOMPLETE - Missing Response Fields**

**Missing Response Fields:**
- ⚠️ Need to verify all alert fields are parsed (may be missing some optional fields)

**Current Implementation:**
```java
// Extracts: id, subject, dateTime, category, status, priority
// Need to verify if all documented fields are captured
```

**Required Fix:**
- Review E*TRADE documentation for all alert fields
- Add any missing fields to `parseAlert()` method

---

### ⚠️ Get Alert Details (`/v1/user/alerts/{id}`)
**Status**: **INCOMPLETE - Missing Response Fields**

**Missing Response Fields:**
- ⚠️ Need to verify all alert detail fields are parsed

**Current Implementation:**
```java
// Extracts: id, subject, dateTime, category, status, priority, message
// Need to verify completeness
```

**Required Fix:**
- Review E*TRADE documentation for all alert detail fields
- Add any missing fields to `parseAlertDetails()` method

---

### ✅ Delete Alerts (`/v1/user/alerts`)
**Status**: Complete
- **Request**: `alertId` array in request body supported
- **Response**: `success` and `message` fields parsed

---

## 4. Market API

### ⚠️ Get Quotes (`/v1/market/quote/{symbols}`)
**Status**: **INCOMPLETE - Missing Parameters**

**Missing Request Parameters:**
- ❌ `requireEarningsDate` - Whether to include earnings date
- ❌ `overrideSymbolCount` - Override symbol count limit
- ❌ `skipMiniOptionsCheck` - Skip mini options check
- ⚠️ `detailFlag` - Hardcoded to "ALL", should be configurable

**Current Implementation:**
```java
// detailFlag is hardcoded to "ALL"
// Missing: requireEarningsDate, overrideSymbolCount, skipMiniOptionsCheck
```

**Required Fix:**
- Add `requireEarningsDate`, `overrideSymbolCount`, `skipMiniOptionsCheck` as optional parameters
- Make `detailFlag` configurable (default to "ALL")

---

### ✅ Look Up Product (`/v1/market/lookup`)
**Status**: Complete
- **Request**: `input` parameter supported
- **Response**: All product fields parsed

---

### ⚠️ Get Option Chains (`/v1/market/optionchains`)
**Status**: **INCOMPLETE - Missing Parameters & Response Fields**

**Missing Request Parameters:**
- ✅ All parameters supported (expiryYear, expiryMonth, expiryDay, strikePriceNear, noOfStrikes, optionCategory, chainType)

**Missing Response Fields:**
- ⚠️ Need to verify all option chain response fields are parsed
- ⚠️ Need to verify `OptionPair` structure is fully parsed

**Current Implementation:**
```java
// Need to verify completeness of option chain parsing
```

**Required Fix:**
- Review E*TRADE documentation for all option chain response fields
- Verify `OptionPair`, `CallOption`, `PutOption` structures are fully parsed

---

### ⚠️ Get Option Expire Dates (`/v1/market/optionexpiredate`)
**Status**: **INCOMPLETE - Missing Parameters**

**Missing Request Parameters:**
- ❌ `expiryType` - Expiry type filter (e.g., "WEEKLY", "MONTHLY")

**Current Implementation:**
```java
// Missing expiryType parameter
```

**Required Fix:**
- Add `expiryType` as optional parameter

---

## 5. Order API

### ⚠️ List Orders (`/v1/accounts/{accountIdKey}/orders`)
**Status**: **INCOMPLETE - Missing Response Fields**

**Missing Response Fields:**
- ⚠️ Need to verify all order fields are parsed
- ⚠️ Need to verify `OrderDetail` array is fully parsed
- ⚠️ Need to verify `Instrument` array is fully parsed
- ⚠️ May be missing some order status fields

**Current Implementation:**
```java
// Parses: orderId, orderType, orderStatus, OrderDetail, Instrument
// Need to verify all fields are captured
```

**Required Fix:**
- Review E*TRADE documentation for all order response fields
- Verify all `OrderDetail` fields are parsed
- Verify all `Instrument` fields are parsed
- Add any missing fields

---

### ⚠️ Preview Order (`/v1/accounts/{accountIdKey}/orders/preview`)
**Status**: **INCOMPLETE - Missing Response Fields**

**Missing Response Fields:**
- ⚠️ Need to verify all preview response fields are parsed
- ⚠️ May be missing some `Order` array fields
- ⚠️ May be missing some `PreviewIds` fields

**Current Implementation:**
```java
// Parses: accountId, PreviewIds, totalOrderValue, estimatedCommission, estimatedTotalAmount, Order
// Need to verify completeness
```

**Required Fix:**
- Review E*TRADE documentation for all preview response fields
- Verify all `Order` fields are parsed
- Verify all `PreviewIds` fields are parsed

---

### ✅ Place Order (`/v1/accounts/{accountIdKey}/orders/place`)
**Status**: Complete
- **Request**: All required fields supported via `OrderRequestBuilder`
- **Response**: `orderIds` and `messages` parsed

---

### ✅ Cancel Order (`/v1/accounts/{accountIdKey}/orders/cancel`)
**Status**: Complete
- **Request**: `orderId` in request body supported
- **Response**: Success and messages parsed

---

### ⚠️ Change Previewed Order (`/v1/accounts/{accountIdKey}/orders/{orderId}/change/preview`)
**Status**: **INCOMPLETE - Missing Response Fields**

**Missing Response Fields:**
- ⚠️ Same as Preview Order - need to verify all fields are parsed

**Required Fix:**
- Same as Preview Order fixes

---

### ⚠️ Place Changed Order (`/v1/accounts/{accountIdKey}/orders/{orderId}/change/place`)
**Status**: **INCOMPLETE - Missing Response Fields**

**Missing Response Fields:**
- ⚠️ Same as Place Order - need to verify all fields are parsed

**Required Fix:**
- Same as Place Order fixes

---

## Summary

### Critical Gaps (High Priority)
1. **Accounts - List Accounts**: Missing `accountMode` and `institutionType` fields
2. **Accounts - Get Balance**: Missing `Cash` and `Margin` sections, missing `accountType` parameter
3. **Accounts - View Portfolio**: Missing 8 query parameters, missing `totalPages` and several position fields
4. **Accounts - List Transactions**: Missing 5 query parameters, missing response metadata fields
5. **Market - Get Quotes**: Missing 3 query parameters, `detailFlag` hardcoded

### Medium Priority Gaps
6. **Accounts - Get Transaction Details**: Missing 2 query parameters, need to verify all fields
7. **Market - Get Option Expire Dates**: Missing `expiryType` parameter
8. **Alerts - List/Details**: Need to verify all fields are captured
9. **Orders - List/Preview/Change**: Need to verify all response fields are parsed

### Low Priority Gaps
10. **Option Chains**: Need to verify all response fields are parsed

---

## Implementation Priority

1. **High Priority**: Fix Accounts API gaps (most critical for core functionality)
2. **Medium Priority**: Fix Market API parameter gaps
3. **Low Priority**: Verify and complete response field parsing for all endpoints

---

## Next Steps

1. Review E*TRADE API documentation for each endpoint to identify exact field names
2. Update client methods to support all documented parameters
3. Update parsing methods to extract all documented response fields
4. Add integration tests to verify all fields are returned
5. Update service/controller layers to expose all parameters
