import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Position } from '../models/position.model';

@Injectable({ providedIn: 'root' })
export class PositionsService {
  private readonly baseUrl = `${environment.apiUrl}/positions`;

  constructor(private http: HttpClient) {}

  list(accountId?: string, openOnly = false): Observable<Position[]> {
    let params = new HttpParams();
    if (accountId) {
      params = params.set('accountId', accountId);
    }
    if (openOnly) {
      params = params.set('openOnly', String(openOnly));
    }
    return this.http.get<Position[]>(this.baseUrl, { params });
  }
}
