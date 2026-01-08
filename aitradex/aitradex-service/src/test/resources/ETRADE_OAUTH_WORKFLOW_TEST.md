# E*TRADE OAuth Workflow Integration Test Guide

This document describes how to run the E*TRADE OAuth workflow integration tests.

## Prerequisites

1. **E*TRADE Sandbox Credentials**
   - Consumer Key (API Key)
   - Consumer Secret (API Secret)
   - Encryption Key (for token encryption, minimum 32 characters)

2. **Environment Variables**
   ```bash
   export ETRADE_CONSUMER_KEY="your_consumer_key"
   export ETRADE_CONSUMER_SECRET="your_consumer_secret"
   export ETRADE_ENCRYPTION_KEY="your_encryption_key_min_32_chars"
   ```

3. **E*TRADE Sandbox Account**
   - A valid E*TRADE sandbox account for authorization

## Running Tests

### Step 1: Request Token Tests (Automated)

The first step tests are fully automated and will run if credentials are configured:

```bash
mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#step1_requestToken_success
mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#step1_requestToken_oauthHeaderFormat
mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#step1_requestToken_callbackUrl
```

These tests validate:
- Request token endpoint response
- OAuth header generation
- Authorization URL format

### Step 2: Authorization URL Tests (Automated)

Tests the authorization URL generation:

```bash
mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#step2_authorizationUrl_format
```

This test will print an authorization URL that you need to open in your browser.

### Step 3: Access Token Exchange (Manual Verifier Required)

For Step 3 tests, you need to manually authorize and provide the `oauth_verifier`:

1. **Run Step 1 to get authorization URL:**
   ```bash
   mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#step2_authorizationUrl_format
   ```

2. **Open the authorization URL in your browser** (from test output)

3. **Log in to E*TRADE sandbox** and authorize the application

4. **Get the `oauth_verifier`:**
   - **Option A (Callback URL):** If callback URL is configured, check the redirect URL:
     ```
     http://localhost:4200/etrade-review-trade/callback?oauth_verifier=<VERIFIER>
     ```
   - **Option B (Out-of-band):** If using out-of-band flow, the verifier will be displayed on the page

5. **Set the verifier as environment variable:**
   ```bash
   export ETRADE_OAUTH_VERIFIER="your_oauth_verifier_from_step_2"
   ```

6. **Run Step 3 tests:**
   ```bash
   mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#step3_accessTokenExchange_manualVerifier
   mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#fullWorkflow_endToEnd
   ```

### Full Workflow Test

To test the complete workflow end-to-end:

```bash
# Step 1: Get authorization URL (run test and note the URL)
mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#step2_authorizationUrl_format

# Step 2: Authorize in browser and get verifier

# Step 3: Set verifier and run full workflow
export ETRADE_OAUTH_VERIFIER="your_verifier"
mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#fullWorkflow_endToEnd
```

## Error Case Tests

These tests validate error handling:

```bash
mvn test -Dtest=EtradeOAuthWorkflowIntegrationTest#errorCase_invalidVerifier
```

**Note:** The invalid credentials and expired token tests are validated but don't actually call the API with invalid credentials to avoid polluting test environment.

## Test Output

Tests provide detailed logging:
- ✅ Success indicators
- ⚠️ Manual steps required
- Request tokens (masked)
- Authorization URLs
- Access tokens (masked)
- Token storage verification

## Troubleshooting

### Test Skipped

If tests are skipped, check:
- Environment variables are set correctly
- Credentials are valid for E*TRADE sandbox
- Network connectivity to `apisb.etrade.com`

### Authorization Failed

- Ensure you're using sandbox credentials (not production)
- Check the callback URL is correctly configured
- Verify the authorization URL is correct

### Access Token Exchange Failed

- Verify the `oauth_verifier` is correct (from Step 2)
- Check that the request token hasn't expired (valid for 5 minutes)
- Ensure you're using the same request token/secret from Step 1

### Token Storage Failed

- Verify database is running (PostgreSQL via Testcontainers)
- Check encryption key is at least 32 characters
- Ensure database migrations have run

## Security Notes

- Never commit credentials to version control
- Use environment variables for all secrets
- Tokens are encrypted before storage
- Test tokens are masked in logs

## Next Steps

After successful OAuth workflow validation:
1. Use the stored access tokens for API calls
2. Test account listing and portfolio retrieval
3. Validate quote retrieval with authenticated tokens
4. Test order placement workflows
