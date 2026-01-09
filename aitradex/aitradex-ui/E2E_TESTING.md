# Playwright E2E Testing Guide

This document describes how to run and write end-to-end (E2E) tests for the aitradex-ui application using Playwright.

## Overview

Playwright is used for E2E testing to validate the complete user experience across the application. Tests run in real browsers and interact with the application as a user would.

## Prerequisites

- Node.js and npm installed
- Application dependencies installed (`npm install`)
- Playwright browsers installed (done automatically on first test run, or manually via `npx playwright install`)

## Running Tests

### Run All Tests (Headless)

```bash
npm run test:e2e
```

This runs all Playwright tests in headless mode (no browser window visible).

### Run Tests in Headed Mode (Visible Browser)

```bash
npm run test:e2e:headed
```

Use this when you want to see the browser interactions during test execution.

### Run Tests with UI Mode

```bash
npm run test:e2e:ui
```

Opens Playwright's interactive test runner UI, which allows you to:
- Run specific tests
- Debug tests step-by-step
- View test timeline and screenshots
- Inspect network requests

### Debug a Test

```bash
npm run test:e2e:debug
```

Launches Playwright Inspector, allowing you to:
- Step through test execution
- Inspect DOM elements
- Execute commands in the browser console
- View network requests and responses

### View HTML Test Report

After running tests, view the HTML report:

```bash
npm run test:e2e:report
```

Opens the last test run's HTML report in your browser, showing:
- Test results (passed/failed)
- Screenshots on failure
- Test execution timeline
- Network logs

## Test Structure

Tests are located in the `e2e/` directory:

```
aitradex-ui/
├── e2e/
│   └── stock-review.spec.ts    # Stock Review feature tests
├── playwright.config.ts         # Playwright configuration
└── package.json                 # Test scripts
```

## Test Configuration

The Playwright configuration (`playwright.config.ts`) includes:

- **Base URL**: `http://localhost:4205` (configurable via `PLAYWRIGHT_BASE_URL` environment variable)
- **Test Directory**: `./e2e`
- **Reporters**: HTML report and console list
- **Browsers**: Chromium, Firefox, WebKit (Safari)
- **Auto-start Dev Server**: Automatically starts `ng serve` before tests (unless server is already running)

## Writing Tests

### Test Selectors

We use `data-testid` attributes for stable, reliable element selection:

```html
<input data-testid="symbol-input" ... />
<button data-testid="search-button" ... />
```

This approach:
- Makes tests resilient to CSS/styling changes
- Improves test readability
- Reduces test maintenance

### Network Mocking

Tests use network route interception to mock API responses, ensuring:
- Tests run deterministically
- No dependency on external APIs
- Fast test execution
- Predictable test data

Example from `stock-review.spec.ts`:

```typescript
await page.route('**/api/stock/quotes/**', async route => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(mockQuoteResponse)
  });
});
```

### Test Example

```typescript
import { test, expect } from '@playwright/test';

test('should search for stock quote', async ({ page }) => {
  // Setup API mocks
  await setupApiMocks(page);
  
  // Navigate to page
  await page.goto('/stock-review');
  
  // Interact with form
  await page.locator('[data-testid="symbol-input"]').fill('AAPL');
  await page.locator('[data-testid="search-button"]').click();
  
  // Verify results
  await expect(page.locator('[data-testid="quote-dashboard"]')).toBeVisible();
  await expect(page.locator('[data-testid="quote-dashboard"]')).toContainText('AAPL');
});
```

## Current Test Coverage

### Stock Review Feature (`e2e/stock-review.spec.ts`)

Tests cover the complete Stock Review user flow:

1. **Page Navigation**: Verifies stock review page loads correctly
2. **Form Validation**: Tests required field validation
3. **Stock Search**: Validates search functionality with mock API responses
4. **Quote Display**: Confirms quote summary and metrics are displayed
5. **Chart Rendering**: Verifies price chart is rendered correctly
6. **Chart Type Switching**: Tests switching between Line, Area, Bar, Candlestick, and OHLC views
7. **Multiple Symbols**: Validates search works with different stock symbols
8. **Error Handling**: Tests error message display on API failures

## Adding New Tests

1. Create a new test file in `e2e/` directory (e.g., `e2e/my-feature.spec.ts`)
2. Import Playwright test utilities: `import { test, expect } from '@playwright/test';`
3. Add `data-testid` attributes to relevant HTML elements
4. Use network route interception for API mocking
5. Write test cases following the existing pattern
6. Run tests: `npm run test:e2e`

## Best Practices

1. **Use data-testid**: Always prefer `data-testid` over CSS selectors or text content
2. **Mock External APIs**: Never rely on real API calls in tests
3. **Independent Tests**: Each test should be independent and not rely on state from other tests
4. **Wait for Elements**: Use `expect().toBeVisible()` instead of fixed timeouts when possible
5. **Clean Setup**: Use `beforeEach` to set up test state (mocks, navigation, etc.)
6. **Descriptive Test Names**: Use clear, descriptive test names that explain what is being tested

## Troubleshooting

### Tests Fail with "Page not found"

- Ensure the dev server is running or that `webServer` is configured in `playwright.config.ts`
- Check the base URL is correct

### Elements Not Found

- Verify `data-testid` attributes are present in the HTML
- Check that elements are rendered before interacting (use `waitFor` or `expect().toBeVisible()`)
- Run in headed mode to see what's happening: `npm run test:e2e:headed`

### API Mocking Not Working

- Verify route patterns match the actual API URLs
- Check network tab in Playwright Inspector to see actual requests
- Ensure route interception is set up before navigation

### Flaky Tests

- Add explicit waits instead of fixed timeouts
- Ensure tests are independent (no shared state)
- Check for race conditions in async operations
- Use `waitForLoadState('networkidle')` when appropriate

## CI/CD Integration

Playwright tests can be integrated into CI/CD pipelines:

```yaml
# Example GitHub Actions
- name: Install Playwright Browsers
  run: npx playwright install --with-deps chromium

- name: Run E2E Tests
  run: npm run test:e2e

- name: Upload Test Results
  uses: actions/upload-artifact@v3
  if: always()
  with:
    name: playwright-report
    path: playwright-report/
```

## Resources

- [Playwright Documentation](https://playwright.dev/)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Playwright Test API](https://playwright.dev/docs/api/class-test)
