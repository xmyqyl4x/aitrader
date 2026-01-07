import { Component } from '@angular/core';
import { AnalyticsService } from '../../core/services/analytics.service';
import { AnalyticsSummary } from '../../core/models/analytics.model';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent {
  accountId = '';
  summary?: AnalyticsSummary;
  error = '';

  constructor(private analyticsService: AnalyticsService) {}

  loadSummary(): void {
    if (!this.accountId) {
      this.error = 'Enter an account ID to load analytics.';
      return;
    }
    this.error = '';
    this.analyticsService.summary(this.accountId).subscribe({
      next: (summary) => (this.summary = summary),
      error: () => (this.error = 'Unable to load summary. Check API connectivity.')
    });
  }
}
