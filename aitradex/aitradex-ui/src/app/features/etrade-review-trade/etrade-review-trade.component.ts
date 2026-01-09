import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

interface EtradeAccount {
  id: string;
  userId: string;
  accountIdKey: string;
  accountType: string;
  accountName: string;
  accountStatus: string;
  linkedAt: string;
  lastSyncedAt: string | null;
}

interface OAuthStatus {
  connected: boolean;
  hasAccounts: boolean;
  accountCount: number;
}

interface AccountBalance {
  accountId: string;
  totalAccountValue: number;
  cashAvailableForInvestment: number;
  cashBalance: number;
  marginBuyingPower?: number;
  netCash?: number;
}

interface PortfolioPosition {
  symbol: string;
  quantity: number;
  currentPrice: number;
  marketValue: number;
  costBasis?: number;
  pnl?: number;
}

interface AccountPortfolio {
  accountId: string;
  positions: PortfolioPosition[];
  totalValue: number;
}

interface Quote {
  symbol: string;
  lastTrade: number;
  bid: number;
  ask: number;
  high: number;
  low: number;
  volume: number;
  changeClose?: number;
  changeClosePercentage?: number;
}

interface Order {
  orderId: string;
  symbol: string;
  orderAction: string;
  priceType: string;
  quantity: number;
  status: string;
  placedTime?: string;
}

@Component({
  selector: 'app-etrade-review-trade',
  templateUrl: './etrade-review-trade.component.html',
  styleUrls: ['./etrade-review-trade.component.scss']
})
export class EtradeReviewTradeComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  oauthStatus: OAuthStatus = {
    connected: false,
    hasAccounts: false,
    accountCount: 0
  };
  
  accounts: EtradeAccount[] = [];
  selectedAccount: EtradeAccount | null = null;
  accountBalance: AccountBalance | null = null;
  accountPortfolio: AccountPortfolio | null = null;

  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  // Order form
  orderForm: FormGroup;

  // Quote form
  quoteForm: FormGroup;
  currentQuote: Quote | null = null;

  // Orders
  orders: Order[] = [];

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.orderForm = this.fb.group({
      symbol: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{1,10}$/)]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      side: ['BUY', Validators.required],
      orderType: ['EQ', Validators.required],
      priceType: ['LIMIT', Validators.required],
      limitPrice: [null],
      stopPrice: [null]
    });
    
    this.quoteForm = this.fb.group({
      symbol: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{1,10}$/)]]
    });
  }

  ngOnInit() {
    // Check for OAuth callback params
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['success']) {
        this.successMessage = 'Account linked successfully!';
        this.checkOAuthStatus();
        this.loadAccounts();
      } else if (params['error']) {
        this.error = params['error'];
      }
    });

    this.checkOAuthStatus();
    this.loadAccounts();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  checkOAuthStatus() {
    this.loading = true;
    this.http.get<OAuthStatus>(`${environment.apiUrl}/api/etrade/oauth/status`)
      .subscribe({
        next: status => {
          this.oauthStatus = status;
          this.loading = false;
        },
        error: err => {
          console.error('Failed to check OAuth status', err);
          this.loading = false;
        }
      });
  }

  initiateOAuth() {
    this.loading = true;
    this.error = null;
    
    this.http.get<{authorizationUrl: string, state: string}>(`${environment.apiUrl}/api/etrade/oauth/authorize`)
      .subscribe({
        next: response => {
          // Redirect to E*TRADE authorization page
          window.location.href = response.authorizationUrl;
        },
        error: err => {
          console.error('Failed to initiate OAuth', err);
          this.error = 'Failed to initiate OAuth authorization: ' + (err.error?.error || err.message);
          this.loading = false;
        }
      });
  }

  loadAccounts() {
    this.loading = true;
    this.error = null; // Clear previous errors
    this.http.get<EtradeAccount[]>(`${environment.apiUrl}/api/etrade/accounts`)
      .subscribe({
        next: accounts => {
          this.accounts = accounts || [];
          if (this.accounts.length > 0) {
            this.selectedAccount = this.accounts[0];
            this.loadAccountDetails();
          }
          this.loading = false;
        },
        error: err => {
          console.error('Failed to load accounts', err);
          // If 404, it means no accounts are linked yet - this is OK
          if (err.status === 404) {
            this.accounts = [];
            this.error = null; // Don't show error for empty accounts
          } else {
            this.error = 'Failed to load accounts: ' + (err.error?.error || err.error?.message || err.message || 'Unknown error');
          }
          this.loading = false;
        }
      });
  }

  selectAccount(account: EtradeAccount) {
    this.selectedAccount = account;
    this.loadAccountDetails();
    this.loadOrders();
  }

  loadAccountDetails() {
    if (!this.selectedAccount) return;
    
    // Load balance
    this.http.get(`${environment.apiUrl}/api/etrade/accounts/${this.selectedAccount.id}/balance`)
      .subscribe({
        next: balance => {
          this.accountBalance = balance;
        },
        error: err => {
          console.error('Failed to load balance', err);
        }
      });
    
    // Load portfolio
    this.http.get(`${environment.apiUrl}/api/etrade/accounts/${this.selectedAccount.id}/portfolio`)
      .subscribe({
        next: portfolio => {
          this.accountPortfolio = portfolio;
        },
        error: err => {
          console.error('Failed to load portfolio', err);
        }
      });
  }

  loadOrders() {
    if (!this.selectedAccount) return;
    
    this.http.get<any>(`${environment.apiUrl}/api/etrade/orders?accountId=${this.selectedAccount.id}`)
      .subscribe({
        next: response => {
          this.orders = response.content || response || [];
        },
        error: err => {
          console.error('Failed to load orders', err);
        }
      });
  }

  getQuote() {
    if (this.quoteForm.invalid || !this.selectedAccount) return;
    
    this.loading = true;
    const symbol = this.quoteForm.value.symbol.toUpperCase();
    
    this.http.get(`${environment.apiUrl}/api/etrade/quotes/${symbol}?accountId=${this.selectedAccount.id}`)
      .subscribe({
        next: quote => {
          this.currentQuote = quote;
          this.loading = false;
        },
        error: err => {
          console.error('Failed to get quote', err);
          this.error = 'Failed to get quote: ' + (err.error?.error || err.message);
          this.loading = false;
        }
      });
  }

  previewOrder() {
    if (this.orderForm.invalid || !this.selectedAccount) return;
    
    this.loading = true;
    const orderRequest = this.buildOrderRequest();
    
    this.http.post(`${environment.apiUrl}/api/etrade/orders/preview?accountId=${this.selectedAccount.id}`, orderRequest)
      .subscribe({
        next: () => {
          this.successMessage = 'Order preview generated successfully';
          this.loading = false;
        },
        error: err => {
          console.error('Failed to preview order', err);
          this.error = 'Failed to preview order: ' + (err.error?.error || err.message);
          this.loading = false;
        }
      });
  }

  placeOrder() {
    if (this.orderForm.invalid || !this.selectedAccount) return;
    
    if (!confirm('Are you sure you want to place this order?')) {
      return;
    }
    
    this.loading = true;
    const orderRequest = this.buildOrderRequest();
    
    this.http.post(`${environment.apiUrl}/api/etrade/orders?accountId=${this.selectedAccount.id}`, orderRequest)
      .subscribe({
        next: order => {
          this.successMessage = 'Order placed successfully!';
          this.orderForm.reset();
          this.loadOrders();
          this.loading = false;
        },
        error: err => {
          console.error('Failed to place order', err);
          this.error = 'Failed to place order: ' + (err.error?.error || err.message);
          this.loading = false;
        }
      });
  }

  cancelOrder(orderId: string) {
    if (!this.selectedAccount || !confirm('Are you sure you want to cancel this order?')) {
      return;
    }
    
    this.http.delete(`${environment.apiUrl}/api/etrade/orders/${orderId}?accountId=${this.selectedAccount.id}`)
      .subscribe({
        next: () => {
          this.successMessage = 'Order cancelled successfully';
          this.loadOrders();
        },
        error: err => {
          console.error('Failed to cancel order', err);
          this.error = 'Failed to cancel order: ' + (err.error?.error || err.message);
        }
      });
  }

  private buildOrderRequest() {
    const formValue = this.orderForm.value;
    return {
      OrderDetail: {
        allOrNone: false,
        priceType: formValue.priceType,
        orderType: formValue.orderType,
        stopPrice: formValue.stopPrice || null,
        limitPrice: formValue.limitPrice || null,
        Instrument: [{
          Product: {
            symbol: formValue.symbol.toUpperCase(),
            securityType: "EQ"
          },
          orderAction: formValue.side,
          quantity: formValue.quantity,
          quantityType: "QUANTITY"
        }]
      }
    };
  }
}
