# Market Data Quote Providers

This document explains the available quote providers and how to configure them.

## Available Providers

### 1. Alpha Vantage (Default - ✅ Enabled)
**Status:** Active and Tested  
**API Key:** `B8IQ3ONTC1RTKRH7`  
**Documentation:** https://www.alphavantage.co/documentation/

Alpha Vantage is the default quote provider. It provides:
- Real-time stock quotes via `GLOBAL_QUOTE` API
- Intraday quotes with 1-minute intervals
- Rate limit: 5 calls per minute (free tier)

**Configuration:**
```yaml
app:
  alpha-vantage:
    api-key: ${ALPHA_VANTAGE_API_KEY:B8IQ3ONTC1RTKRH7}
    base-url: https://www.alphavantage.co
    enabled: true
  market-data:
    default-source: alphavantage
```

**Environment Variables:**
- `ALPHA_VANTAGE_API_KEY` - Your Alpha Vantage API key
- `ALPHA_VANTAGE_BASE_URL` - API base URL (default: https://www.alphavantage.co)
- `ALPHA_VANTAGE_ENABLED` - Enable/disable adapter (default: true)

### 2. Yahoo Finance (❌ Disabled by Default)
**Status:** Unreliable - May fail  
**Reason:** Yahoo Finance's unofficial API is unstable and frequently changes

**To Enable:**
```yaml
app:
  market-data:
    providers:
      yahoo:
        enabled: true
```

Or set environment variable:
```bash
APP_MARKET_DATA_PROVIDERS_YAHOO_ENABLED=true
```

### 3. Stooq (❌ Disabled by Default)
**Status:** Unreliable - May fail  
**Reason:** Stooq CSV endpoint is inconsistent and may not have real-time data

**To Enable:**
```yaml
app:
  market-data:
    providers:
      stooq:
        enabled: true
```

Or set environment variable:
```bash
APP_MARKET_DATA_PROVIDERS_STOOQ_ENABLED=true
```

### 4. Quote Snapshots (✅ Enabled)
**Status:** Always available  
**Source:** Database-backed quote snapshots

This adapter reads from the `quote_snapshots` table and is always enabled as a fallback.

## Changing the Default Provider

### Via Environment Variable:
```bash
export APP_MARKET_DATA_DEFAULT_SOURCE=alphavantage
```

### Via application.yml:
```yaml
app:
  market-data:
    default-source: alphavantage  # or yahoo, stooq, quote-snapshots
```

## Priority Order

When fetching quotes, providers are tried in priority order:

1. **alphavantage** (priority 1) - Default, most reliable
2. **yahoo** (priority 2) - Disabled by default
3. **stooq** (priority 3) - Disabled by default  
4. **quote-snapshots** (priority 4) - Fallback to database

## Streaming Quotes

The `QuoteStreamingService` polls quotes at regular intervals:

```yaml
app:
  market-data:
    stream-poll-interval: 10s    # How often to fetch quotes
    stream-max-duration: 5m      # Auto-expire subscriptions after
```

### REST API Endpoints:

```bash
# Stream quotes via Server-Sent Events (SSE)
GET /api/quotes/stream/{symbol}?source=alphavantage

# Create a subscription
POST /api/quotes/stream/subscribe?symbol=AAPL&pollIntervalMs=10000&maxDurationMs=300000

# Get subscription status
GET /api/quotes/stream/subscription/{subscriptionId}

# Cancel subscription
DELETE /api/quotes/stream/subscription/{subscriptionId}

# Get streaming statistics
GET /api/quotes/stream/stats
```

## Testing

### Unit Tests
All providers have comprehensive unit tests that run with every build:
- `AlphaVantageAdapterTest` - 11 tests
- `YahooFinanceAdapterTest` - 11 tests  
- `StooqAdapterTest` - 12 tests
- `QuoteSnapshotAdapterTest` - 7 tests

### Integration Test
Alpha Vantage has a real API integration test that only runs when the API key is available:

```bash
# Run integration test
ALPHA_VANTAGE_API_KEY=your_key_here mvn test -Dtest=AlphaVantageAdapterIntegrationTest

# Or with the default key
mvn test -Dtest=AlphaVantageAdapterIntegrationTest
```

The integration test validates:
- Real API connectivity
- Correct response parsing
- Rate limit handling
- Multiple symbol queries

**Note:** Unit tests are sufficient for validation. Integration tests only need to run once to verify the API works.

## Rate Limits

### Alpha Vantage (Free Tier)
- 5 API calls per minute
- 500 API calls per day
- The adapter handles rate limit responses gracefully

### Best Practices
1. Use caching to reduce API calls (configured via `cache-ttl`)
2. For high-frequency updates, consider upgrading Alpha Vantage plan
3. Use quote snapshots for historical data

## Troubleshooting

### "Alpha Vantage API rate limit reached"
Wait 60 seconds before making more requests, or upgrade your API key.

### "Quote for SYMBOL not found"
- Verify the symbol is valid
- Check if markets are open
- Try a different provider

### Yahoo/Stooq fails frequently
These providers are disabled by default due to unreliability. Use Alpha Vantage or Quote Snapshots instead.

## Examples

### Fetch from default provider (Alpha Vantage):
```bash
curl http://localhost:8080/api/market-data/quotes/latest?symbol=AAPL
```

### Fetch from specific provider:
```bash
curl "http://localhost:8080/api/market-data/quotes/latest?symbol=AAPL&source=alphavantage"
```

### List available providers:
```bash
curl http://localhost:8080/api/market-data/sources
```

### Check provider health:
```bash
curl http://localhost:8080/api/market-data/health
```
