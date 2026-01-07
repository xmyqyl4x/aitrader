import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnalyticsSummary, EquityPoint, SymbolPnl } from '../models/analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly baseUrl = `${environment.apiUrl}/analytics`;

  constructor(private http: HttpClient) {}

  summary(accountId: string): Observable<AnalyticsSummary> {
    const params = new HttpParams().set('accountId', accountId);
    return this.http.get<AnalyticsSummary>(`${this.baseUrl}/summary`, { params });
  }

  equity(accountId: string, startDate?: string, endDate?: string): Observable<EquityPoint[]> {
    let params = new HttpParams().set('accountId', accountId);
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }
    return this.http.get<EquityPoint[]>(`${this.baseUrl}/equity`, { params });
  }

  pnl(accountId: string, source?: string): Observable<SymbolPnl[]> {
    let params = new HttpParams().set('accountId', accountId);
    if (source) {
      params = params.set('source', source);
    }
    return this.http.get<SymbolPnl[]>(`${this.baseUrl}/pnl`, { params });
  }
}
