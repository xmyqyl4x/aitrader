import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EtradeService, EtradeAccount } from '../../../core/services/etrade.service';

@Component({
  selector: 'app-etrade-account-details',
  templateUrl: './etrade-account-details.component.html',
  styleUrls: ['./etrade-account-details.component.scss']
})
export class EtradeAccountDetailsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  account: EtradeAccount | null = null;
  accountIdKey: string | null = null;
  loading = false;
  error: string | null = null;

  constructor(
    private etradeService: EtradeService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.accountIdKey = params['accountIdKey'] || params['id'];
      if (this.accountIdKey) {
        this.loadAccountDetails();
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAccountDetails() {
    if (!this.accountIdKey) return;

    this.loading = true;
    this.error = null;

    // First, try to find account by accountIdKey from the accounts list
    this.etradeService.getAccounts().subscribe({
      next: accounts => {
        const account = accounts.find(a => 
          a.accountIdKey === this.accountIdKey || a.id === this.accountIdKey
        );
        
        if (account) {
          // Try to get full account details
          this.etradeService.getAccount(account.id).subscribe({
            next: accountDetails => {
              this.account = accountDetails;
              this.loading = false;
            },
            error: err => {
              // If getAccount fails, use the account from the list
              this.account = account;
              this.loading = false;
            }
          });
        } else {
          this.error = 'Account not found';
          this.loading = false;
        }
      },
      error: err => {
        console.error('Failed to load account details', err);
        this.error = 'Failed to load account details: ' + (err.error?.error || err.message);
        this.loading = false;
      }
    });
  }

  goBack() {
    this.router.navigate(['/etrade-review-trade']);
  }

  connectAccount() {
    if (!this.account) return;
    
    this.loading = true;
    this.error = null;

    this.etradeService.connectAccount(this.account.id).subscribe({
      next: accounts => {
        // Refresh account details
        this.loadAccountDetails();
      },
      error: err => {
        this.error = 'Failed to connect account: ' + (err.error?.error || err.message);
        this.loading = false;
      }
    });
  }
}
