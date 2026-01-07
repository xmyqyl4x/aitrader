import { Component } from '@angular/core';
import { AnalyticsService } from '../../core/services/analytics.service';
import { AnalyticsSummary, EquityPoint, SymbolPnl } from '../../core/models/analytics.model';

@Component({
  selector: 'app-analytics',
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.css']
})
export class AnalyticsComponent {
  accountId = '';
  summary?: AnalyticsSummary;
  equity: EquityPoint[] = [];
  pnl: SymbolPnl[] = [];
  error = '';

  constructor(private analyticsService: AnalyticsService) {}

  load(): void {
    if (!this.accountId) {
      this.error = 'Enter an account ID to load analytics.';
      return;
    }
    this.error = '';
    this.analyticsService.summary(this.accountId).subscribe({
      next: (summary) => (this.summary = summary),
      error: () => (this.error = 'Unable to load summary.')
    });
    this.analyticsService.equity(this.accountId).subscribe({
      next: (equity) => (this.equity = equity),
      error: () => (this.error = 'Unable to load equity curve.')
    });
    this.analyticsService.pnl(this.accountId).subscribe({
      next: (pnl) => (this.pnl = pnl),
      error: () => (this.error = 'Unable to load PnL detail.')
    });
  }
}
