import { test, expect } from '@playwright/test';

test.describe('E*TRADE Review & Trade UI', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the E*TRADE Review & Trade page
    await page.goto('/etrade-review-trade');
  });

  test('should display navigation menu and Link Account button', async ({ page }) => {
    // Verify navigation menu appears
    await expect(page.locator('.nav-tabs')).toBeVisible();
    
    // Verify all navigation items are present
    await expect(page.locator('text=Accounts')).toBeVisible();
    await expect(page.locator('text=Market')).toBeVisible();
    await expect(page.locator('text=Order')).toBeVisible();
    await expect(page.locator('text=Alerts')).toBeVisible();
    await expect(page.locator('text=Research and Analysis')).toBeVisible();
    await expect(page.locator('text=Algorithm')).toBeVisible();
    await expect(page.locator('text=Quant')).toBeVisible();

    // Verify status banner appears
    await expect(page.locator('.alert')).toBeVisible();
    await expect(page.locator('text=Connection Status')).toBeVisible();

    // Verify Link Account button appears (when not connected)
    const linkButton = page.locator('button:has-text("Link E*TRADE Account")');
    await expect(linkButton).toBeVisible();
  });

  test('should navigate between sections', async ({ page }) => {
    // Click on Market tab
    await page.click('text=Market');
    await expect(page.locator('text=Market Section')).toBeVisible();

    // Click on Order tab
    await page.click('text=Order');
    await expect(page.locator('text=Order Section')).toBeVisible();

    // Click on Alerts tab
    await page.click('text=Alerts');
    await expect(page.locator('text=Alerts Section')).toBeVisible();

    // Return to Accounts tab
    await page.click('text=Accounts');
    await expect(page.locator('text=E*TRADE Accounts')).toBeVisible();
  });

  test('should load accounts when Link Account is clicked', async ({ page }) => {
    // Mock the API response for accounts
    await page.route('**/api/etrade/accounts', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: '1',
            userId: 'user1',
            accountIdKey: 'key1',
            accountType: 'BROKERAGE',
            accountName: 'Test Account',
            accountStatus: 'ACTIVE',
            linkedAt: '2024-01-01T00:00:00Z',
            lastSyncedAt: '2024-01-02T00:00:00Z'
          }
        ])
      });
    });

    // Mock OAuth status
    await page.route('**/api/etrade/oauth/status', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          connected: true,
          hasAccounts: true,
          accountCount: 1
        })
      });
    });

    // Click Link Account button
    await page.click('button:has-text("Link E*TRADE Account")');

    // Wait for accounts table to appear
    await expect(page.locator('text=Test Account')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('table')).toBeVisible();
  });

  test('should display account list table with correct columns', async ({ page }) => {
    // Mock accounts API
    await page.route('**/api/etrade/accounts', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: '1',
            userId: 'user1',
            accountIdKey: 'key1',
            accountType: 'BROKERAGE',
            accountName: 'Test Account',
            accountStatus: 'ACTIVE',
            linkedAt: '2024-01-01T00:00:00Z',
            lastSyncedAt: '2024-01-02T00:00:00Z'
          }
        ])
      });
    });

    await page.route('**/api/etrade/oauth/status', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          connected: true,
          hasAccounts: true,
          accountCount: 1
        })
      });
    });

    // Load accounts
    await page.click('button:has-text("Link E*TRADE Account")');
    await page.waitForSelector('table');

    // Verify table columns
    await expect(page.locator('th:has-text("Account Name")')).toBeVisible();
    await expect(page.locator('th:has-text("Account Type")')).toBeVisible();
    await expect(page.locator('th:has-text("Account ID Key")')).toBeVisible();
    await expect(page.locator('th:has-text("Status")')).toBeVisible();
    await expect(page.locator('th:has-text("Connection Status")')).toBeVisible();
    await expect(page.locator('th:has-text("Authorization Status")')).toBeVisible();
    await expect(page.locator('th:has-text("Last Synced")')).toBeVisible();
    await expect(page.locator('th:has-text("Actions")')).toBeVisible();

    // Verify account data
    await expect(page.locator('td:has-text("Test Account")')).toBeVisible();
    await expect(page.locator('td:has-text("BROKERAGE")')).toBeVisible();
  });

  test('should navigate to account details page when View Details is clicked', async ({ page }) => {
    // Mock accounts API
    await page.route('**/api/etrade/accounts', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: '1',
            userId: 'user1',
            accountIdKey: 'key1',
            accountType: 'BROKERAGE',
            accountName: 'Test Account',
            accountStatus: 'ACTIVE',
            linkedAt: '2024-01-01T00:00:00Z',
            lastSyncedAt: '2024-01-02T00:00:00Z'
          }
        ])
      });
    });

    await page.route('**/api/etrade/oauth/status', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          connected: true,
          hasAccounts: true,
          accountCount: 1
        })
      });
    });

    // Mock account details API
    await page.route('**/api/etrade/accounts/1', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '1',
          userId: 'user1',
          accountIdKey: 'key1',
          accountType: 'BROKERAGE',
          accountName: 'Test Account',
          accountStatus: 'ACTIVE',
          linkedAt: '2024-01-01T00:00:00Z',
          lastSyncedAt: '2024-01-02T00:00:00Z'
        })
      });
    });

    // Load accounts
    await page.click('button:has-text("Link E*TRADE Account")');
    await page.waitForSelector('table');

    // Click View Details
    await page.click('button:has-text("View Details")');

    // Verify navigation to details page
    await expect(page).toHaveURL(/.*\/etrade-review-trade\/accounts\/key1/);
    await expect(page.locator('text=Account Details')).toBeVisible();
    await expect(page.locator('text=Test Account')).toBeVisible();
  });

  test('should initiate OAuth flow when Connect Account is clicked with invalid token', async ({ page }) => {
    // Mock accounts API
    await page.route('**/api/etrade/accounts', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: '1',
            userId: 'user1',
            accountIdKey: 'key1',
            accountType: 'BROKERAGE',
            accountName: 'Test Account',
            accountStatus: 'ACTIVE',
            linkedAt: '2024-01-01T00:00:00Z',
            lastSyncedAt: null
          }
        ])
      });
    });

    await page.route('**/api/etrade/oauth/status', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          connected: false,
          hasAccounts: false,
          accountCount: 0
        })
      });
    });

    // Mock sync accounts to fail with 401 (token invalid)
    await page.route('**/api/etrade/accounts/sync*', async route => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Token expired' })
      });
    });

    // Mock OAuth authorize endpoint
    let oauthInitiated = false;
    await page.route('**/api/etrade/oauth/authorize', async route => {
      oauthInitiated = true;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authorizationUrl: 'https://apisb.etrade.com/oauth/authorize?token=test',
          state: 'test-state'
        })
      });
    });

    // Load accounts
    await page.click('button:has-text("Link E*TRADE Account")');
    await page.waitForSelector('table');

    // Click Connect Account
    await page.click('button:has-text("Connect Account")');

    // Wait for OAuth to be initiated
    await page.waitForTimeout(1000);

    // Verify OAuth was initiated (would redirect in real scenario)
    expect(oauthInitiated).toBe(true);
  });

  test('should successfully connect account when token is valid', async ({ page }) => {
    // Mock accounts API
    await page.route('**/api/etrade/accounts', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: '1',
            userId: 'user1',
            accountIdKey: 'key1',
            accountType: 'BROKERAGE',
            accountName: 'Test Account',
            accountStatus: 'ACTIVE',
            linkedAt: '2024-01-01T00:00:00Z',
            lastSyncedAt: null
          }
        ])
      });
    });

    await page.route('**/api/etrade/oauth/status', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          connected: true,
          hasAccounts: true,
          accountCount: 1
        })
      });
    });

    // Mock successful sync
    await page.route('**/api/etrade/accounts/sync*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: '1',
            userId: 'user1',
            accountIdKey: 'key1',
            accountType: 'BROKERAGE',
            accountName: 'Test Account',
            accountStatus: 'ACTIVE',
            linkedAt: '2024-01-01T00:00:00Z',
            lastSyncedAt: '2024-01-02T00:00:00Z'
          }
        ])
      });
    });

    // Load accounts
    await page.click('button:has-text("Link E*TRADE Account")');
    await page.waitForSelector('table');

    // Click Connect Account
    await page.click('button:has-text("Connect Account")');

    // Wait for success message
    await expect(page.locator('.alert-success:has-text("connected successfully")')).toBeVisible({ timeout: 5000 });
  });

  test('should display error message when account loading fails', async ({ page }) => {
    // Mock accounts API to fail
    await page.route('**/api/etrade/accounts', async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Server error' })
      });
    });

    await page.route('**/api/etrade/oauth/status', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          connected: false,
          hasAccounts: false,
          accountCount: 0
        })
      });
    });

    // Click Link Account
    await page.click('button:has-text("Link E*TRADE Account")');

    // Verify error message appears
    await expect(page.locator('.alert-danger')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('text=/Failed to load accounts/')).toBeVisible();
  });

  test('should show empty state when no accounts exist', async ({ page }) => {
    // Mock empty accounts response
    await page.route('**/api/etrade/accounts', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([])
      });
    });

    await page.route('**/api/etrade/oauth/status', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          connected: false,
          hasAccounts: false,
          accountCount: 0
        })
      });
    });

    // Click Link Account
    await page.click('button:has-text("Link E*TRADE Account")');

    // Verify empty state
    await expect(page.locator('text=No E*TRADE Accounts Linked')).toBeVisible();
    await expect(page.locator('text=Link your E*TRADE account')).toBeVisible();
  });
});
