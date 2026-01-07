import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { OrdersComponent } from './features/orders/orders.component';
import { PositionsComponent } from './features/positions/positions.component';
import { UploadsComponent } from './features/uploads/uploads.component';
import { AnalyticsComponent } from './features/analytics/analytics.component';
import { AuditComponent } from './features/audit/audit.component';

const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'orders', component: OrdersComponent },
  { path: 'positions', component: PositionsComponent },
  { path: 'uploads', component: UploadsComponent },
  { path: 'analytics', component: AnalyticsComponent },
  { path: 'audit', component: AuditComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
