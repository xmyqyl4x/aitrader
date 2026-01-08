# E*TRADE API Test Lessons Learned

This document captures key lessons, patterns, and best practices learned from creating and running E*TRADE API tests. Use these patterns for future E*TRADE API test implementations.

## Table of Contents
1. [Test Architecture](#test-architecture)
2. [OAuth Authentication Pattern](#oauth-authentication-pattern)
3. [XML Response Validation](#xml-response-validation)
4. [Error Handling & Graceful Skipping](#error-handling--graceful-skipping)
5. [Environment Variable Management](#environment-variable-management)
6. [Standalone Test Approach](#standalone-test-approach)
7. [Common Patterns & Utilities](#common-patterns--utilities)
8. [Common Pitfalls & Solutions](#common-pitfalls--solutions)

---

## Test Architecture

### Key Principles

1. **Standalone Tests (No Docker/Spring Boot Context)**
   - Tests should run independently without requiring database or Spring Boot context
   - Use `StandaloneEtradeApiClient` for API calls
   - Manually initialize OAuth components when needed

2. **Test Structure Pattern**
   ```java
   @DisplayName("E*TRADE [Feature] API Tests (Standalone)")
   @EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_KEY", matches = ".+")
   @EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_SECRET", matches = ".+")
   class Etrade[Feature]ApiTest {
     
     private StandaloneEtradeApiClient apiClient;
     private String baseUrl;
     
     @BeforeEach
     void setUp() {
       // Initialize client with credentials
       // Handle access token retrieval/validation
     }
     
     @Test
     @DisplayName("[Feature] - Success")
     void [feature]_success() {
       // Make API call
       // Validate XML response
       // Assert required fields
     }
   }
   ```

3. **Test Organization**
   - One test class per API endpoint group (Accounts, Orders, Quotes, etc.)
   - Each test validates both structure and content
   - Include both success and response structure tests

---

## OAuth Authentication Pattern

### Access Token Handling

**Pattern 1: Direct Token (Preferred for CI/CD)**
```java
String accessToken = System.getenv("ETRADE_ACCESS_TOKEN");
String accessTokenSecret = System.getenv("ETRADE_ACCESS_TOKEN_SECRET");
// Use directly if available
```

**Pattern 2: Automatic Token Exchange (For Development)**
```java
if (accessToken == null || accessTokenSecret == null) {
  String verifier = System.getenv("ETRADE_OAUTH_VERIFIER");
  if (verifier != null && !verifier.isEmpty()) {
    EtradeAccessTokenHelper.AccessTokenPair tokens = 
        EtradeAccessTokenHelper.getAccessToken(consumerKey, consumerSecret, verifier);
    accessToken = tokens.getAccessToken();
    accessTokenSecret = tokens.getAccessTokenSecret();
  }
}
```

### Key Learnings

1. **Token Exchange Requires Request Token First**
   - Always get a fresh request token before exchanging for access token
   - Request tokens expire after 5 minutes
   - Access tokens expire at midnight US Eastern time

2. **OAuth 1.0a Signature Requirements**
   - Must include `oauth_callback` in Authorization header for request token
   - Sandbox requires `"oob"` (out-of-band) for callback URL
   - All OAuth parameters must be included in signature base string

3. **Token Storage**
   - For standalone tests, tokens are not persisted
   - Use `EtradeAccessTokenHelper` for one-time token exchange
   - Production code should encrypt and store tokens in database

---

## XML Response Validation

### Validation Utility Pattern

Use `XmlResponseValidator` for all XML parsing and validation:

```java
// Parse XML
Document doc = XmlResponseValidator.parseXml(responseXml);

// Validate root element
XmlResponseValidator.validateRootElement(doc, "ExpectedRootElement");

// Get root element
Element root = doc.getDocumentElement();

// Extract child elements
Element childElement = XmlResponseValidator.getFirstChildElement(root, "ChildName");
List<Element> childElements = XmlResponseValidator.getChildElements(root, "ChildName");

// Validate required fields
String fieldValue = XmlResponseValidator.getRequiredField(element, "fieldName");

// Validate numeric fields
double numericValue = XmlResponseValidator.getNumericField(element, "fieldName");
double numericValueWithDefault = XmlResponseValidator.getNumericFieldOrDefault(
    element, "fieldName", 0.0);

// Validate URL fields
XmlResponseValidator.validateUrlField(element, "urlFieldName");
```

### Validation Checklist

For each API endpoint test, validate:
1. ✅ HTTP 200 response
2. ✅ Valid XML structure (parses without errors)
3. ✅ Correct root element name
4. ✅ Required child elements exist
5. ✅ Required fields are non-empty
6. ✅ Numeric fields parse correctly (even if zero)
7. ✅ URL fields (if present) are valid URLs

---

## Error Handling & Graceful Skipping

### Critical Pattern: Use `assumeTrue` Not `AssertionError`

**❌ WRONG (Causes test failures):**
```java
if (accessToken == null) {
  throw new AssertionError("Access token required");
}
```

**✅ CORRECT (Gracefully skips test):**
```java
if (accessToken == null) {
  org.junit.jupiter.api.Assumptions.assumeTrue(false, 
      "ETRADE_ACCESS_TOKEN must be set");
}
```

### Error Handling Best Practices

1. **Missing Credentials**: Use `assumeTrue(false, "message")` to skip gracefully
2. **API Errors**: Let exceptions propagate (they'll be caught by test framework)
3. **Network Errors**: Tests will fail with clear error messages
4. **Invalid Responses**: Use assertions to fail tests with descriptive messages

### Test Result States

- **PASS**: Test executed and all assertions passed
- **SKIP**: Test skipped due to missing prerequisites (credentials, tokens)
- **FAIL**: Test executed but assertions failed or exceptions occurred

---

## Environment Variable Management

### Required Variables

```bash
# Always Required
ETRADE_CONSUMER_KEY="your_consumer_key"
ETRADE_CONSUMER_SECRET="your_consumer_secret"

# Option 1: Direct Access Tokens (Preferred)
ETRADE_ACCESS_TOKEN="your_access_token"
ETRADE_ACCESS_TOKEN_SECRET="your_access_token_secret"

# Option 2: OAuth Verifier (For Development)
ETRADE_OAUTH_VERIFIER="verifier_from_browser"

# Optional
ETRADE_BASE_URL="https://apisb.etrade.com"  # Defaults to sandbox
ETRADE_ENCRYPTION_KEY="key_for_token_encryption"
```

### Variable Access Pattern

```java
String consumerKey = System.getenv("ETRADE_CONSUMER_KEY");
String consumerSecret = System.getenv("ETRADE_CONSUMER_SECRET");
String accessToken = System.getenv("ETRADE_ACCESS_TOKEN");
String accessTokenSecret = System.getenv("ETRADE_ACCESS_TOKEN_SECRET");
String baseUrl = System.getenv().getOrDefault("ETRADE_BASE_URL", "https://apisb.etrade.com");
```

### Security Notes

- **Never commit** access tokens or secrets to version control
- Use environment variables or secure configuration files
- Rotate tokens regularly
- Use sandbox credentials for testing
- Production tokens should be stored encrypted

---

## Standalone Test Approach

### Why Standalone?

1. **No Docker Required**: Tests can run in any environment
2. **Faster Execution**: No container startup time
3. **Simpler Setup**: No database migrations or Spring Boot context
4. **CI/CD Friendly**: Easier to run in automated pipelines

### Standalone Components

1. **StandaloneEtradeApiClient**
   - Handles OAuth 1.0a signing
   - Makes HTTP requests
   - No Spring dependencies

2. **EtradeAccessTokenHelper**
   - Obtains access tokens programmatically
   - No database required
   - Can be used in tests or utilities

3. **XmlResponseValidator**
   - Pure Java XML parsing
   - No external dependencies
   - Reusable across all tests

### Initialization Pattern

```java
// Create properties
EtradeProperties properties = new EtradeProperties();
properties.setConsumerKey(consumerKey);
properties.setConsumerSecret(consumerSecret);
properties.setBaseUrl(baseUrl);
properties.setEnvironment(EtradeProperties.Environment.SANDBOX);

// Create OAuth components
EtradeOAuth1Template oauthTemplate = new EtradeOAuth1Template(
    consumerKey, consumerSecret);
EtradeTokenEncryption tokenEncryption = new EtradeTokenEncryption(encryptionKey);

// Create API client
StandaloneEtradeApiClient apiClient = new StandaloneEtradeApiClient(
    baseUrl, consumerKey, consumerSecret, accessToken, accessTokenSecret);
```

---

## Common Patterns & Utilities

### 1. API Request Pattern

```java
// Simple GET request
String responseXml = apiClient.get("/v1/endpoint");

// GET with query parameters
Map<String, String> params = Map.of(
    "param1", "value1",
    "param2", "value2"
);
String responseXml = apiClient.get("/v1/endpoint", params);
```

### 2. Response Validation Pattern

```java
@Test
@DisplayName("Feature - Success")
void feature_success() {
    // Make API request
    String responseXml = apiClient.get("/v1/endpoint");
    
    // Validate HTTP response
    assertNotNull(responseXml, "Response should not be null");
    assertFalse(responseXml.trim().isEmpty(), "Response should not be empty");
    
    // Parse XML
    Document doc = XmlResponseValidator.parseXml(responseXml);
    
    // Validate root element
    XmlResponseValidator.validateRootElement(doc, "ExpectedRoot");
    Element root = doc.getDocumentElement();
    
    // Validate required fields
    String field1 = XmlResponseValidator.getRequiredField(root, "field1");
    String field2 = XmlResponseValidator.getRequiredField(root, "field2");
    
    // Log results
    log.info("✅ Test passed - all required fields validated");
}
```

### 3. Account ID Key Retrieval Pattern

For tests that need an `accountIdKey`:

```java
private String getFirstAccountIdKey() {
    try {
        String responseXml = apiClient.get("/v1/accounts/list");
        Document doc = XmlResponseValidator.parseXml(responseXml);
        Element root = doc.getDocumentElement();
        Element accountsElement = XmlResponseValidator.getFirstChildElement(root, "Accounts");
        if (accountsElement != null) {
            Element firstAccount = XmlResponseValidator.getFirstChildElement(
                accountsElement, "Account");
            if (firstAccount != null) {
                return XmlResponseValidator.getTextContent(firstAccount, "accountIdKey");
            }
        }
    } catch (Exception e) {
        log.warn("Failed to get accountIdKey: {}", e.getMessage());
    }
    return null;
}
```

### 4. Logging Pattern

```java
log.info("=== Testing [Feature] API ===");
log.info("Response received (length: {} chars)", responseXml.length());
log.info("Found {} item(s)", items.size());
log.info("  Field: {}", fieldValue);
log.info("✅ Test passed - all required fields validated");
```

---

## Common Pitfalls & Solutions

### Pitfall 1: Tests Fail Instead of Skip

**Problem**: Using `throw new AssertionError()` causes test failures

**Solution**: Use `org.junit.jupiter.api.Assumptions.assumeTrue(false, "message")`

### Pitfall 2: OAuth Callback URL Mismatch

**Problem**: E*TRADE sandbox rejects callback URLs

**Solution**: Use `"oob"` (out-of-band) for sandbox environment:
```java
if (environment == Environment.SANDBOX) {
    return "oob";
}
```

### Pitfall 3: Request Token Expiration

**Problem**: Request tokens expire after 5 minutes

**Solution**: Always get a fresh request token before exchanging for access token

### Pitfall 4: Missing OAuth Parameters in Header

**Problem**: `oauth_callback` not included in Authorization header

**Solution**: Ensure OAuth-specific parameters are included in header:
```java
if (parameters != null && parameters.containsKey("oauth_callback")) {
    oauthParams.put("oauth_callback", parameters.get("oauth_callback"));
}
```

### Pitfall 5: XML Parsing Errors

**Problem**: XML structure doesn't match expectations

**Solution**: 
- Always validate root element first
- Check if elements are arrays or single objects
- Use `isArray()` and `isObject()` checks before parsing

### Pitfall 6: Query Parameter Encoding

**Problem**: Special characters in query parameters cause issues

**Solution**: URL encode query parameters:
```java
String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
```

### Pitfall 7: Numeric Field Validation

**Problem**: Zero values or missing fields cause test failures

**Solution**: Use `getNumericFieldOrDefault()` with default values:
```java
double value = XmlResponseValidator.getNumericFieldOrDefault(
    element, "fieldName", 0.0);
assertTrue(Double.isFinite(value), "Value should be finite");
```

---

## Test Execution Commands

### Run All API Tests
```bash
mvn test -Dtest="EtradeAccountsApiTest,EtradeAccountBalanceApiTest,EtradePortfolioApiTest"
```

### Run Single Test Class
```bash
mvn test -Dtest=EtradeAccountsApiTest
```

### Run Single Test Method
```bash
mvn test -Dtest=EtradeAccountsApiTest#listAccounts_success
```

### With Environment Variables (PowerShell)
```powershell
$env:ETRADE_CONSUMER_KEY="key"
$env:ETRADE_CONSUMER_SECRET="secret"
$env:ETRADE_ACCESS_TOKEN="token"
$env:ETRADE_ACCESS_TOKEN_SECRET="token_secret"
mvn test -Dtest=EtradeAccountsApiTest
```

---

## Future Test Implementation Checklist

When creating new E*TRADE API tests:

- [ ] Create test class following naming convention: `Etrade[Feature]ApiTest`
- [ ] Add `@EnabledIfEnvironmentVariable` for required credentials
- [ ] Implement `setUp()` method with token handling
- [ ] Use `StandaloneEtradeApiClient` for API calls
- [ ] Use `XmlResponseValidator` for response parsing
- [ ] Validate root element, required fields, and structure
- [ ] Use `assumeTrue()` for graceful skipping (not `AssertionError`)
- [ ] Add comprehensive logging for debugging
- [ ] Test both success and structure validation
- [ ] Document required environment variables
- [ ] Handle edge cases (empty responses, missing fields, etc.)

---

## Key Takeaways

1. **Standalone is Better**: No Docker/Spring Boot context needed for API tests
2. **Graceful Skipping**: Use `assumeTrue()` to skip tests when credentials are missing
3. **XML Validation**: Always validate structure before accessing fields
4. **OAuth Patterns**: Request token → Authorization → Access token exchange
5. **Error Messages**: Provide clear, actionable error messages
6. **Logging**: Comprehensive logging helps debugging
7. **Reusability**: Create utilities (`XmlResponseValidator`, `StandaloneEtradeApiClient`) for reuse
8. **Security**: Never commit tokens, use environment variables

---

## Related Files

- `StandaloneEtradeApiClient.java` - API client for standalone tests
- `XmlResponseValidator.java` - XML parsing and validation utilities
- `EtradeAccessTokenHelper.java` - OAuth token exchange helper
- `EtradeAccountsApiTest.java` - Example test implementation
- `ETRADE_API_TESTS.md` - Test execution documentation

---

*Last Updated: 2026-01-08*
*Based on: E*TRADE Accounts & Portfolio API Tests Implementation*
