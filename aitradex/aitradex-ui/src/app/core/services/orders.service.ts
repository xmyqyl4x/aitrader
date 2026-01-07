import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Order } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly baseUrl = `${environment.apiUrl}/orders`;

  constructor(private http: HttpClient) {}

  list(accountId?: string, status?: string): Observable<Order[]> {
    let params = new HttpParams();
    if (accountId) {
      params = params.set('accountId', accountId);
    }
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Order[]>(this.baseUrl, { params });
  }

  create(payload: Record<string, unknown>): Observable<Order> {
    return this.http.post<Order>(this.baseUrl, payload);
  }
}
