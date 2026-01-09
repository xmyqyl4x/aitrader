import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EtradeService, EtradeAccount, OAuthStatus } from '../../core/services/etrade.service';

@Component({
  selector: 'app-etrade-review-trade',
  templateUrl: './etrade-review-trade.component.html',
  styleUrls: ['./etrade-review-trade.component.scss']
})
export class EtradeReviewTradeComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Navigation
  activeSection: 'market' | 'order' | 'alerts' | 'research' | 'algorithm' | 'quant' | 'accounts' = 'accounts';

  // OAuth Status
  oauthStatus: OAuthStatus = {
    connected: false,
    hasAccounts: false,
    accountCount: 0
  };

  // Account Management
  accounts: EtradeAccount[] = [];
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  // OAuth Flow State
  oauthFlowActive = false;
  oauthVerifier: string = '';
  showVerifierInput = false;

  constructor(
    private etradeService: EtradeService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    // Check for OAuth callback params
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['success']) {
        this.successMessage = 'Account linked successfully!';
        this.checkOAuthStatus();
        this.loadAccounts();
      } else if (params['error']) {
        this.error = this.getErrorMessage(params['error']);
        this.oauthFlowActive = false;
      }
    });

    this.checkOAuthStatus();
    this.loadAccounts();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ============================================================================
  // Navigation
  // ============================================================================

  setActiveSection(section: 'market' | 'order' | 'alerts' | 'research' | 'algorithm' | 'quant' | 'accounts') {
    this.activeSection = section;
  }

  // ============================================================================
  // OAuth Status
  // ============================================================================

  checkOAuthStatus() {
    console.log('[EtradeReviewTrade] checkOAuthStatus - starting');
    this.loading = true;
    this.etradeService.getOAuthStatus().subscribe({
      next: status => {
        console.log('[EtradeReviewTrade] checkOAuthStatus - success, status:', status);
        this.oauthStatus = status;
        this.loading = false;
      },
      error: err => {
        console.error('[EtradeReviewTrade] checkOAuthStatus - error:', err);
        this.oauthStatus = {
          connected: false,
          hasAccounts: false,
          accountCount: 0,
          tokenStatus: 'MISSING'
        };
        this.loading = false;
      }
    });
  }

  getStatusBannerClass(): string {
    if (!this.oauthStatus.connected) {
      return 'alert-warning';
    }
    if (this.oauthStatus.tokenStatus === 'EXPIRED' || this.oauthStatus.tokenStatus === 'INVALID') {
      return 'alert-danger';
    }
    return 'alert-success';
  }

  getStatusBannerText(): string {
    if (!this.oauthStatus.connected) {
      return 'Not Linked';
    }
    if (this.oauthStatus.tokenStatus === 'EXPIRED') {
      return 'Token Expired';
    }
    if (this.oauthStatus.tokenStatus === 'INVALID') {
      return 'Token Invalid';
    }
    if (this.oauthStatus.tokenStatus === 'MISSING') {
      return 'Token Missing';
    }
    return 'Authorized';
  }

  // ============================================================================
  // Account Management
  // ============================================================================

  loadAccounts() {
    console.log('[EtradeReviewTrade] loadAccounts - starting');
    this.loading = true;
    this.error = null;
    this.etradeService.getAccounts().subscribe({
      next: accounts => {
        console.log('[EtradeReviewTrade] loadAccounts - success, received accounts:', accounts);
        this.accounts = accounts || [];
        console.log('[EtradeReviewTrade] loadAccounts - accounts array length:', this.accounts.length);
        this.loading = false;
        this.checkOAuthStatus(); // Refresh status after loading accounts
      },
      error: err => {
        console.error('[EtradeReviewTrade] loadAccounts - error:', err);
        console.error('[EtradeReviewTrade] loadAccounts - error status:', err.status);
        console.error('[EtradeReviewTrade] loadAccounts - error message:', err.message);
        console.error('[EtradeReviewTrade] loadAccounts - error body:', err.error);
        if (err.status === 404) {
          this.accounts = [];
          this.error = null;
          console.log('[EtradeReviewTrade] loadAccounts - 404, no accounts found (this is OK)');
        } else {
          this.error = 'Failed to load accounts: ' + (err.error?.error || err.error?.message || err.message || 'Unknown error');
          console.error('[EtradeReviewTrade] loadAccounts - setting error:', this.error);
        }
        this.loading = false;
      }
    });
  }

  viewAccountDetails(account: EtradeAccount) {
    this.router.navigate(['/etrade-review-trade/accounts', account.accountIdKey || account.id]);
  }

  connectAccount(account: EtradeAccount) {
    console.log('[EtradeReviewTrade] connectAccount - starting for account:', account);
    this.loading = true;
    this.error = null;
    this.successMessage = null;

    // Step 1: Check for valid token by trying to sync accounts
    console.log('[EtradeReviewTrade] connectAccount - calling connectAccount service with accountId:', account.id);
    this.etradeService.connectAccount(account.id).subscribe({
      next: (accounts: EtradeAccount[]) => {
        console.log('[EtradeReviewTrade] connectAccount - success, received accounts:', accounts);
        // Success - token is valid and account is connected
        this.successMessage = `Account "${account.accountName || account.accountIdKey}" connected successfully!`;
        console.log('[EtradeReviewTrade] connectAccount - success message:', this.successMessage);
        this.loadAccounts(); // Refresh account list
        this.loading = false;
      },
      error: (err: any) => {
        console.error('[EtradeReviewTrade] connectAccount - error:', err);
        console.error('[EtradeReviewTrade] connectAccount - error status:', err.status);
        // Token might be missing or expired - initiate OAuth flow
        if (err.status === 401 || err.status === 403 || err.error?.message?.includes('Token')) {
          console.log('[EtradeReviewTrade] connectAccount - token invalid, initiating OAuth flow');
          this.initiateOAuthFlow(account);
        } else {
          this.error = 'Failed to connect account: ' + (err.error?.error || err.error?.message || err.message || 'Unknown error');
          console.error('[EtradeReviewTrade] connectAccount - setting error:', this.error);
          this.loading = false;
        }
      }
    });
  }

  // ============================================================================
  // OAuth Flow
  // ============================================================================

  initiateOAuthFlow(account?: EtradeAccount) {
    console.log('[EtradeReviewTrade] initiateOAuthFlow - starting', account ? 'for account: ' + account.accountIdKey : '');
    this.loading = true;
    this.error = null;
    this.oauthFlowActive = true;

    this.etradeService.initiateOAuth().subscribe({
      next: response => {
        console.log('[EtradeReviewTrade] initiateOAuthFlow - success, authorizationUrl:', response.authorizationUrl);
        // Redirect to E*TRADE authorization page
        window.location.href = response.authorizationUrl;
      },
      error: err => {
        console.error('[EtradeReviewTrade] initiateOAuthFlow - error:', err);
        this.error = 'Failed to initiate OAuth authorization: ' + (err.error?.error || err.message);
        this.loading = false;
        this.oauthFlowActive = false;
      }
    });
  }

  submitOAuthVerifier() {
    if (!this.oauthVerifier || this.oauthVerifier.trim() === '') {
      this.error = 'Please enter the verification code';
      return;
    }

    // The OAuth callback will be handled by the backend redirect
    // For manual verifier entry, we'd need a separate endpoint
    // For now, redirect to callback with verifier
    this.error = 'Manual verifier entry not yet implemented. Please use the callback flow.';
  }

  // ============================================================================
  // Link Account Button
  // ============================================================================

  onLinkAccountClick() {
    console.log('[EtradeReviewTrade] onLinkAccountClick - button clicked');
    this.loadAccounts();
    this.setActiveSection('accounts');
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  getAccountConnectionStatus(account: EtradeAccount): string {
    // This would be determined by checking token status for the account
    // For now, return based on oauthStatus
    if (this.oauthStatus.connected && this.oauthStatus.hasAccounts) {
      return 'Connected';
    }
    return 'Not Connected';
  }

  getAccountAuthorizationStatus(account: EtradeAccount): string {
    if (this.oauthStatus.tokenStatus === 'EXPIRED') {
      return 'Token Expired';
    }
    if (this.oauthStatus.tokenStatus === 'INVALID') {
      return 'Token Invalid';
    }
    if (this.oauthStatus.tokenStatus === 'MISSING') {
      return 'Missing';
    }
    if (this.oauthStatus.connected) {
      return 'Authorized';
    }
    return 'Not Authorized';
  }

  private getErrorMessage(errorCode: string): string {
    const errorMessages: { [key: string]: string } = {
      'authorization_denied': 'Authorization was denied. Please try again.',
      'invalid_callback': 'Invalid OAuth callback. Please try again.',
      'token_not_found': 'OAuth token not found. Please initiate the flow again.',
      'callback_failed': 'OAuth callback failed. Please try again.'
    };
    return errorMessages[errorCode] || `Error: ${errorCode}`;
  }
}
