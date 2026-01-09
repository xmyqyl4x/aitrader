import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { OrdersComponent } from './features/orders/orders.component';
import { PositionsComponent } from './features/positions/positions.component';
import { UploadsComponent } from './features/uploads/uploads.component';
import { AnalyticsComponent } from './features/analytics/analytics.component';
import { AuditComponent } from './features/audit/audit.component';
import { StockReviewComponent } from './features/stock-review/stock-review.component';
import { StockReviewStackComponent } from './features/stock-review/stock-review-stack.component';
import { EtradeReviewTradeComponent } from './features/etrade-review-trade/etrade-review-trade.component';
import { EtradeAccountDetailsComponent } from './features/etrade-review-trade/etrade-account-details/etrade-account-details.component';

const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'orders', component: OrdersComponent },
  { path: 'positions', component: PositionsComponent },
  { path: 'uploads', component: UploadsComponent },
  { path: 'analytics', component: AnalyticsComponent },
  { path: 'audit', component: AuditComponent },
  { path: 'stock-review', component: StockReviewComponent },
  { path: 'stock-review/searches', component: StockReviewStackComponent },
  { path: 'etrade-review-trade', component: EtradeReviewTradeComponent },
  { path: 'etrade-review-trade/accounts/:accountIdKey', component: EtradeAccountDetailsComponent },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
