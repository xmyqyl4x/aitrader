import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuditLog } from '../models/audit-log.model';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly baseUrl = `${environment.apiUrl}/audit`;

  constructor(private http: HttpClient) {}

  list(actor?: string, entityRef?: string): Observable<AuditLog[]> {
    let params = new HttpParams();
    if (actor) {
      params = params.set('actor', actor);
    }
    if (entityRef) {
      params = params.set('entityRef', entityRef);
    }
    return this.http.get<AuditLog[]>(this.baseUrl, { params });
  }
}
