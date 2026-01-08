import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgApexchartsModule } from 'ng-apexcharts';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { OrdersComponent } from './features/orders/orders.component';
import { PositionsComponent } from './features/positions/positions.component';
import { UploadsComponent } from './features/uploads/uploads.component';
import { AnalyticsComponent } from './features/analytics/analytics.component';
import { AuditComponent } from './features/audit/audit.component';
import { StockReviewComponent } from './features/stock-review/stock-review.component';
import { StockReviewStackComponent } from './features/stock-review/stock-review-stack.component';
import { StockChartComponent } from './features/stock-review/stock-chart.component';
import { EtradeReviewTradeComponent } from './features/etrade-review-trade/etrade-review-trade.component';

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    OrdersComponent,
    PositionsComponent,
    UploadsComponent,
    AnalyticsComponent,
    AuditComponent,
    StockReviewComponent,
    StockReviewStackComponent,
    StockChartComponent,
    EtradeReviewTradeComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    NgApexchartsModule,
    FontAwesomeModule,
    NgbModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {}
