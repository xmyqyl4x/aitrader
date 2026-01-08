# E*TRADE Accounts & Portfolio API Tests

This document describes how to run the standalone E*TRADE API tests for Accounts and Portfolio endpoints.

## Prerequisites

### 1. E*TRADE Credentials

You need the following credentials:

- **Consumer Key** (API Key)
- **Consumer Secret** (API Secret)
- **Access Token** (from OAuth workflow)
- **Access Token Secret** (from OAuth workflow)

### 2. Obtaining Access Token

To obtain an access token for testing:

1. **Run OAuth Workflow Test:**
   ```bash
   mvn test -Dtest=EtradeOAuthWorkflowStandaloneTest#step2_authorizationUrl_format
   ```

2. **Open the authorization URL** from the test output in your browser

3. **Log in to E*TRADE sandbox** and authorize the application

4. **Get the oauth_verifier** from the callback URL or displayed page

5. **Run Access Token Exchange:**
   ```bash
   $env:ETRADE_OAUTH_VERIFIER="your_verifier_here"
   mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#step3_accessTokenExchange_manualVerifier
   ```

6. **Extract Access Token and Secret:**
   - From test output logs (masked)
   - Or from database if using full integration test
   - Or manually from the OAuth callback response

### 3. Environment Variables

Set the following environment variables:

```bash
# Required
export ETRADE_CONSUMER_KEY="your_consumer_key"
export ETRADE_CONSUMER_SECRET="your_consumer_secret"
export ETRADE_ACCESS_TOKEN="your_access_token"
export ETRADE_ACCESS_TOKEN_SECRET="your_access_token_secret"

# Optional
export ETRADE_BASE_URL="https://apisb.etrade.com"  # Defaults to sandbox
```

**Windows PowerShell:**
```powershell
$env:ETRADE_CONSUMER_KEY="your_consumer_key"
$env:ETRADE_CONSUMER_SECRET="your_consumer_secret"
$env:ETRADE_ACCESS_TOKEN="your_access_token"
$env:ETRADE_ACCESS_TOKEN_SECRET="your_access_token_secret"
```

## Running Tests

### Run All API Tests

```bash
mvn test -Dtest=EtradeAccountsApiTest,EtradeAccountBalanceApiTest,EtradePortfolioApiTest
```

### Run Individual Test Classes

**List Accounts:**
```bash
mvn test -Dtest=EtradeAccountsApiTest
```

**Get Account Balance:**
```bash
mvn test -Dtest=EtradeAccountBalanceApiTest
```

**View Portfolio:**
```bash
mvn test -Dtest=EtradePortfolioApiTest
```

### Run Specific Test Methods

```bash
mvn test -Dtest=EtradeAccountsApiTest#listAccounts_success
mvn test -Dtest=EtradeAccountBalanceApiTest#getAccountBalance_success
mvn test -Dtest=EtradePortfolioApiTest#viewPortfolio_success
```

## Test Coverage

### 1. List Accounts API

**Endpoint:** `GET /v1/accounts/list`

**Validations:**
- ✅ HTTP 200 response
- ✅ Valid XML response
- ✅ Root element: `<AccountListResponse>`
- ✅ Contains `<Accounts>` with at least one `<Account>`
- ✅ Required fields per account:
  - `accountId`
  - `accountIdKey`
  - `accountMode`
  - `accountDesc`
  - `accountName`
  - `accountType`
  - `institutionType`
  - `accountStatus`

### 2. Get Account Balance API

**Endpoint:** `GET /v1/accounts/{accountIdKey}/balance`

**Validations:**
- ✅ HTTP 200 response
- ✅ Valid XML response
- ✅ Root element: `<BalanceResponse>`
- ✅ Required fields:
  - `accountId`
  - `accountType`
  - `accountDescription`
  - `accountMode`
- ✅ Required sections:
  - `<Cash>`
  - `<Computed>`
  - `<Margin>`
- ✅ Numeric fields parse correctly (e.g., `netCash`, `cashBuyingPower`)

### 3. View Portfolio API

**Endpoint:** `GET /v1/accounts/{accountIdKey}/portfolio`

**Validations:**
- ✅ HTTP 200 response
- ✅ Valid XML response
- ✅ Root element: `<PortfolioResponse>`
- ✅ Contains `<AccountPortfolio>`
- ✅ Required fields:
  - `accountId`
  - `totalPages` (numeric)
- ✅ For each `<Position>`:
  - `positionId`
  - `quantity`
  - `positionType`
  - `marketValue`
  - `<Product><symbol>`
  - `<Product><securityType>`
- ✅ URL fields validated (if present):
  - `quoteDetails`
  - `lotsDetails`

## Troubleshooting

### Access Token Expired

E*TRADE access tokens expire at midnight US Eastern time. If tests fail with authentication errors:

1. Re-run the OAuth workflow to get a new access token
2. Update `ETRADE_ACCESS_TOKEN` and `ETRADE_ACCESS_TOKEN_SECRET` environment variables

### No Accounts Found

If `List Accounts` returns no accounts:

- Verify you're using a sandbox account with test accounts
- Check that the access token is valid
- Ensure the account is active

### XML Parsing Errors

If you see XML parsing errors:

- Check that the response is actually XML (not HTML error page)
- Verify the API endpoint URL is correct
- Check network connectivity to E*TRADE API

### Authentication Errors

If you see 401/403 errors:

- Verify access token and secret are correct
- Check that tokens haven't expired
- Ensure consumer key/secret are correct
- Verify OAuth signature is being generated correctly

## Test Output

Tests will log:
- ✅ Success messages for each validation
- Account details (IDs, types, etc.)
- Position information (if any)
- Any validation failures with clear error messages

Example output:
```
INFO  - === Testing List Accounts API ===
INFO  - Response received (length: 1234 chars)
INFO  - Found 2 account(s)
INFO  - Validating account #1
INFO  -   Account ID: 123456789
INFO  -   Account ID Key: abc123
INFO  - ✅ List Accounts test passed - all required fields validated
```

## Security Notes

- **Never commit** access tokens or secrets to version control
- Use environment variables or secure configuration files
- Rotate tokens regularly
- Use sandbox credentials for testing
- Production tokens should be stored encrypted
