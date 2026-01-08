# E*TRADE Integration Guide

## Overview

The E*TRADE integration provides full OAuth 1.0 authentication and API access to E*TRADE trading functionality. This allows users to:

- Link E*TRADE accounts via OAuth
- View account balances and portfolios
- Retrieve real-time stock quotes
- Place, preview, and cancel orders
- Track order history

## Configuration

### Required Environment Variables

```bash
# E*TRADE API Credentials (Required)
ETRADE_CONSUMER_KEY=a83b0321f09e97fc8f4315ad5fbcd489
ETRADE_CONSUMER_SECRET=c4d304698d156d4c3681c73de0c4e400060cac46ee1504259b324695daa77dd4

# Environment (SANDBOX or PRODUCTION)
ETRADE_ENVIRONMENT=SANDBOX

# Callback URL for OAuth
ETRADE_CALLBACK_URL=http://localhost:4200/etrade-review-trade/callback

# Token Encryption Key (Required for production - minimum 32 characters)
ETRADE_ENCRYPTION_KEY=your-secure-encryption-key-minimum-32-characters-long

# Enable/Disable E*TRADE Integration
ETRADE_ENABLED=true
```

### Application Configuration

The configuration is defined in `application.yml`:

```yaml
app:
  etrade:
    consumer-key: ${ETRADE_CONSUMER_KEY}
    consumer-secret: ${ETRADE_CONSUMER_SECRET}
    callback-url: ${ETRADE_CALLBACK_URL:http://localhost:4200/etrade-review-trade/callback}
    environment: ${ETRADE_ENVIRONMENT:SANDBOX}
    base-url: ${ETRADE_BASE_URL:https://apisb.etrade.com}
    authorize-url: ${ETRADE_AUTHORIZE_URL:https://us.etrade.com/e/t/etws/authorize}
    enabled: ${ETRADE_ENABLED:true}
    encryption-key: ${ETRADE_ENCRYPTION_KEY:default-encryption-key-change-in-production-min-32-chars}
```

### Environment-Specific URLs

**Sandbox (Development):**
- Base URL: `https://apisb.etrade.com`
- Authorization URL: `https://us.etrade.com/e/t/etws/authorize`

**Production:**
- Base URL: `https://api.etrade.com`
- Authorization URL: `https://us.etrade.com/e/t/etws/authorize`

The environment is automatically configured based on `ETRADE_ENVIRONMENT`.

## Database Schema

The integration uses the following database tables (created via Liquibase migration `0004-etrade-integration.yaml`):

### `etrade_account`
Stores linked E*TRADE accounts.

### `etrade_oauth_token`
Stores encrypted OAuth access tokens and secrets.

### `etrade_order`
Tracks placed orders and their status.

### `etrade_audit_log`
Logs all E*TRADE API calls for auditing and debugging.

## Security

### Token Encryption

OAuth tokens are encrypted at rest using AES-256 encryption. The encryption key must be:

- At least 32 characters long
- Stored securely (environment variable, secret management service)
- Never committed to version control

### Secrets Management

- **Never** hardcode API keys or secrets
- Use environment variables or secure secret management (AWS Secrets Manager, HashiCorp Vault, etc.)
- Secrets are never logged or exposed in error messages
- OAuth tokens are encrypted before storage

## API Endpoints

### OAuth Flow

```
GET  /api/etrade/oauth/authorize          - Initiates OAuth flow
GET  /api/etrade/oauth/callback           - OAuth callback handler
GET  /api/etrade/oauth/status             - Get OAuth connection status
```

### Accounts

```
GET    /api/etrade/accounts               - List user accounts
GET    /api/etrade/accounts/{accountId}   - Get account details
GET    /api/etrade/accounts/{accountId}/balance    - Get account balance
GET    /api/etrade/accounts/{accountId}/portfolio  - Get account portfolio
POST   /api/etrade/accounts/sync          - Sync accounts from E*TRADE
DELETE /api/etrade/accounts/{accountId}   - Unlink account
```

### Quotes

```
GET /api/etrade/quotes/{symbol}?accountId={accountId}  - Get quote for symbol
GET /api/etrade/quotes?symbols={symbols}&accountId={accountId} - Get multiple quotes
```

### Orders

```
POST   /api/etrade/orders/preview?accountId={accountId} - Preview order
POST   /api/etrade/orders?accountId={accountId}         - Place order
GET    /api/etrade/orders?accountId={accountId}         - List orders
GET    /api/etrade/orders/{orderId}?accountId={accountId} - Get order details
DELETE /api/etrade/orders/{orderId}?accountId={accountId}  - Cancel order
```

## OAuth Flow

### Step 1: Initiate OAuth

User clicks "Link E*TRADE Account" button in UI, which calls:

```
GET /api/etrade/oauth/authorize
```

This returns an authorization URL that redirects the user to E*TRADE's authorization page.

### Step 2: User Authorization

User authorizes the application on E*TRADE's website.

### Step 3: OAuth Callback

E*TRADE redirects back to:

```
GET /api/etrade/oauth/callback?oauth_token={token}&oauth_verifier={verifier}
```

The backend:
1. Exchanges request token + verifier for access token
2. Retrieves account list from E*TRADE
3. Links accounts to the user
4. Stores encrypted access tokens
5. Redirects to the frontend success page

## Frontend Usage

### Route

Access the E*TRADE Review & Trade page at:

```
/etrade-review-trade
```

### Features

1. **Account Linking**: Click "Link E*TRADE Account" to start OAuth flow
2. **Account Selection**: Select from linked accounts (if multiple)
3. **View Balance**: See account balance and available cash
4. **View Portfolio**: See current positions
5. **Get Quotes**: Search for stock quotes
6. **Place Orders**: Create and place orders with preview
7. **Manage Orders**: View and cancel open orders

## Testing

### Backend Tests

Unit tests cover:
- OAuth 1.0 signing and request generation
- Token encryption/decryption
- API client error handling
- Rate limiting and retries

Integration tests (with mocked E*TRADE API):
- OAuth flow
- Account operations
- Quote retrieval
- Order placement and cancellation

### Frontend Tests

- Component unit tests
- Service tests with mocked HTTP
- E2E tests for OAuth flow and order placement

### Running Tests

```bash
# Backend unit tests
cd aitradex-service
mvn test

# Frontend unit tests
cd aitradex-ui
npm test

# E2E tests
npm run test:e2e
```

## Troubleshooting

### OAuth Authorization Fails

1. Check `ETRADE_CONSUMER_KEY` and `ETRADE_CONSUMER_SECRET` are correct
2. Verify `ETRADE_CALLBACK_URL` matches registered callback in E*TRADE developer portal
3. Ensure using correct environment (sandbox vs production)
4. Check logs for OAuth errors

### Token Storage Issues

1. Verify `ETRADE_ENCRYPTION_KEY` is set and at least 32 characters
2. Check database connection and `etrade_oauth_token` table exists
3. Review audit logs for token-related errors

### API Rate Limiting

E*TRADE has rate limits:
- **Sandbox**: 100 requests/minute
- **Production**: 360 requests/hour

The client automatically retries with exponential backoff on 429 errors. Check audit logs for rate limit events.

### Account Not Found Errors

1. Verify account is linked via OAuth flow
2. Check `etrade_account` table for account entries
3. Ensure access token is valid and not expired
4. Try re-linking account if token expired

## Development Notes

### Request Token Storage

Currently, request tokens are stored in-memory in the OAuth controller. In production, this should be moved to:

- Redis (recommended for distributed systems)
- Session storage
- Database with expiration

### Account ID Management

For MVP, account IDs are generated temporarily. In production:

- Use actual E*TRADE account IDs from account list API
- Map E*TRADE account IDs to internal account IDs
- Support multiple accounts per user

### Error Handling

All E*TRADE API errors are:
- Logged to audit log
- Returned as user-friendly error messages
- Retried automatically for transient errors (network, rate limits)

### Security Best Practices

1. **Never log secrets or tokens** - All logging filters out sensitive data
2. **Use HTTPS in production** - Required for OAuth callback
3. **Rotate encryption keys** - Change encryption key periodically
4. **Monitor audit logs** - Review for suspicious activity
5. **Token expiration** - Handle token refresh when E*TRADE adds support

## Production Deployment

### Pre-deployment Checklist

- [ ] Set `ETRADE_ENVIRONMENT=PRODUCTION`
- [ ] Configure production consumer key/secret
- [ ] Set secure `ETRADE_ENCRYPTION_KEY` (min 32 chars)
- [ ] Update `ETRADE_CALLBACK_URL` to production URL
- [ ] Verify callback URL is registered in E*TRADE developer portal
- [ ] Enable HTTPS for callback URL
- [ ] Configure request token storage (Redis recommended)
- [ ] Set up monitoring for audit logs
- [ ] Test OAuth flow in production sandbox first

### Monitoring

Monitor the following:
- `etrade_audit_log` table for API errors
- Rate limit responses (429 status codes)
- OAuth token expiration
- Account linking success/failure rates

## API Rate Limits

| Environment | Limit | Window |
|------------|-------|--------|
| Sandbox    | 100   | 1 minute |
| Production | 360   | 1 hour |

The client automatically handles rate limits with exponential backoff.

## Support

For E*TRADE API documentation:
- https://developer.etrade.com/

For issues with this integration:
- Check audit logs in `etrade_audit_log` table
- Review application logs
- Verify configuration values
