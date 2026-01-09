# E*TRADE Alerts API Documentation

## Overview

This document describes the E*TRADE Alerts API integration in the `aitradex` application, including API endpoints, persistence behavior, and testing approach.

## Prerequisite: Valid OAuth Access Token

All Alerts API calls require a **valid OAuth 1.0a access token + token secret**. The access token:
- Expires by default at end of day (US Eastern time)
- Can become inactive after ~2 hours of inactivity and must be renewed
- Must be included in the `Authorization: OAuth ...` header for all requests

## Alerts API Flow

### Step 1: List Alerts (Inbox)

**Endpoint**: `GET /v1/user/alerts`

**Query Parameters**:
- `count` (default 25, max 300) - Number of alerts to return
- `category` (default STOCK + ACCOUNT) - Alert category filter (STOCK, ACCOUNT)
- `status` (default READ + UNREAD) - Alert status filter (READ, UNREAD, DELETED)
- `direction` - Sort direction (ASC, DESC)
- `search` - Subject search term

**Response**: `AlertsResponse` containing:
- `totalAlerts` - Total number of alerts
- `Alert[]` - Array of alert records, each with:
  - `id` - Alert ID (numeric)
  - `createTime` - Creation timestamp (epoch milliseconds)
  - `subject` - Alert subject
  - `status` - Alert status (UNREAD, READ, DELETED, UNDELETED)

**Special Cases**:
- If no alerts, API may return **404** with error code **53** ("no alerts in inbox")

**Database Persistence**:
- **Upsert alerts** by `(accountId, alertId)` combination (unique constraint)
- Update fields: `createTime`, `subject`, `status`, `lastSyncedAt`
- Treat List Alerts as the "source of truth" for current status

**Application Endpoint**: `GET /api/etrade/alerts?accountId={accountId}`

### Step 2: Get Alert Details

**Endpoint**: `GET /v1/user/alerts/{id}`

**Path Parameters**:
- `id` (required) - Alert ID (must be > 0)

**Query Parameters**:
- `htmlTags` (optional, default false) - If true, return msgText with HTML tags

**Response**: `AlertDetailsResponse` containing:
- `id` - Alert ID
- `createTime` - Creation timestamp (epoch milliseconds)
- `subject` - Alert subject
- `msgText` - Alert message text
- `readTime` - Read timestamp (epoch milliseconds, 0 if not read)
- `deleteTime` - Delete timestamp (epoch milliseconds, 0 if not deleted)
- `symbol` - Market symbol (if stock alert)
- `next` - URL for next alert
- `prev` - URL for previous alert

**Database Persistence**:
- **Upsert alert details** by `alertId`:
  - Store `msgText`, `readTime`, `deleteTime`, `symbol`, `next`, `prev`
  - Set `detailsFetchedAt` timestamp
- If `deleteTime > 0`, mark alert status as DELETED in main alerts table

**Application Endpoint**: `GET /api/etrade/alerts/{alertId}?accountId={accountId}&tags={boolean}`

### Step 3: Delete Alerts

**Endpoint**: `DELETE /v1/user/alerts/{alert_id_list}`

**Path Parameters**:
- `alert_id_list` (required) - Comma-separated list of alert IDs (e.g., `6772,6774`)

**Response**: `DeleteAlertsResponse` containing:
- `result` - Result status (e.g., "Success")
- `FailedAlerts[]` - Array of failed alerts (if any), each with:
  - `id` - Failed alert ID
  - `reason` - Failure reason

**Database Persistence**:
- For each successfully deleted alert:
  - Update alert status to `DELETED`
  - Set `lastSyncedAt` timestamp
  - Create `DELETE_SUCCESS` event record
- For failed alerts:
  - Create `DELETE_FAILURE` event record with error message
  - Do not update alert status

**Application Endpoint**: `DELETE /api/etrade/alerts/{alertIdList}?accountId={accountId}`

## Database Schema

### `etrade_alert` Table

**Key Fields**:
- `id` - Internal UUID (primary key)
- `account_id` - Internal account UUID (foreign key to `etrade_account`)
- `alert_id` - E*TRADE alert ID (numeric)
- `create_time` - Creation timestamp (epoch milliseconds)
- `subject` - Alert subject
- `status` - Alert status (UNREAD, READ, DELETED, UNDELETED)
- `last_synced_at` - Last sync timestamp
- `created_at` / `updated_at` - Audit timestamps

**Unique Constraint**: `(account_id, alert_id)` - Ensures upsert logic works correctly

**Indexes**:
- `idx_etrade_alert_account_id` - For account-based queries
- `idx_etrade_alert_alert_id` - For alert ID lookups
- `idx_etrade_alert_status` - For status-based queries
- `idx_etrade_alert_last_synced_at` - For sync tracking (descending)

### `etrade_alert_detail` Table

**Key Fields**:
- `id` - Internal UUID (primary key)
- `alert_id` - Foreign key to `etrade_alert.id`
- `msg_text` - Alert message text
- `read_time` - Read timestamp (epoch milliseconds)
- `delete_time` - Delete timestamp (epoch milliseconds)
- `symbol` - Market symbol (if stock alert)
- `next_url` - Next alert URL
- `prev_url` - Previous alert URL
- `details_fetched_at` - Details fetch timestamp
- `created_at` / `updated_at` - Audit timestamps

**Indexes**:
- `idx_etrade_alert_detail_alert_id` - For alert-based queries

### `etrade_alert_event` Table

**Key Fields**:
- `id` - Internal UUID (primary key)
- `alert_id` - Foreign key to `etrade_alert.id`
- `event_type` - Event type (DELETE_ATTEMPT, DELETE_SUCCESS, DELETE_FAILURE, SYNC_RUN, etc.)
- `event_status` - Event status (SUCCESS, FAILURE, PENDING)
- `error_message` - Error message (if failure)
- `event_data` - Additional event data (JSONB)
- `created_at` - Event creation timestamp

**Indexes**:
- `idx_etrade_alert_event_alert_id` - For alert-based queries
- `idx_etrade_alert_event_type` - For event type queries

## Testing Approach

### Functional Tests

All Alerts API tests are **functional tests** that:
- Call our application's REST API endpoints (not E*TRADE directly)
- Make real calls to E*TRADE sandbox (not mocked)
- Validate database persistence after each API call
- Use `assumeTrue` to skip tests when credentials are not available

### Test Coverage

1. **Token Prerequisite Enforcement** (`test0_tokenPrerequisiteEnforcement_alertsApiRequiresOAuthToken`)
   - Validates that Alerts API requires OAuth token
   - Tests error handling for non-existent accounts

2. **List Alerts Happy Path** (`test1_listAlerts_happyPath`)
   - Calls List Alerts endpoint
   - Validates response structure (200 or 404 for empty inbox)
   - Validates database persistence (upsert by accountId + alertId)

3. **Get Alert Details Happy Path** (`test2_getAlertDetails_happyPath`)
   - Calls Get Alert Details endpoint
   - Validates response structure
   - Validates database persistence (upsert alert details)

4. **Delete Alerts Happy Path** (`test3_deleteAlerts_happyPath`)
   - Calls Delete Alerts endpoint
   - Validates response structure
   - Validates database state updates (status set to DELETED, events created)

5. **Invalid Alert ID** (`test4_getAlertDetails_invalidAlertId`)
   - Tests error handling for invalid alert ID (0 or negative)

6. **Filters and Pagination** (`test5_listAlerts_filtersAndPagination`)
   - Tests List Alerts with various filters and pagination parameters

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

**Run Tests**:
```bash
cd aitradex-service
mvn test -Dtest=EtradeAlertsFunctionalTest
```

**Expected Results**:
- Tests that require credentials will be skipped if credentials are not available
- Tests that run will validate API calls and database persistence
- All tests should pass when credentials are properly configured
- Tests gracefully handle empty inbox (404 responses)

## Implementation Notes

### Service Layer

- `EtradeAlertsService.listAlerts()` - Calls API and persists alerts (upsert)
- `EtradeAlertsService.getAlertDetails()` - Calls API and persists alert details (upsert)
- `EtradeAlertsService.deleteAlerts()` - Calls API and updates alert status + creates events

### Transaction Management

- All service methods use `@Transactional(noRollbackFor = EtradeApiException.class)` to prevent rollback on API exceptions
- This allows the exception handler to return proper HTTP error responses

### Error Handling

- `EtradeApiException` is thrown for API errors
- `ApiExceptionHandler` catches `EtradeApiException` and returns structured error responses
- Database persistence failures are logged but don't break API calls
- Empty inbox (404 with error code 53) is handled gracefully

### Special Cases

- **Empty Inbox**: When List Alerts returns 404 (no alerts), the application handles this gracefully without errors
- **Invalid Alert ID**: Alert ID must be > 0; validation errors are returned as 400/404
- **Failed Deletions**: Failed alert deletions are tracked in event log but don't prevent successful deletions

## Removed Mock Tests

The following mock-based tests have been removed and replaced with functional tests:
- `EtradeAlertsApiIntegrationTest` - Mock-based integration tests
- `EtradeApiClientAlertsAPITest` - Mock-based unit tests

All Alerts API testing is now done through functional tests that call our application's REST API endpoints.
