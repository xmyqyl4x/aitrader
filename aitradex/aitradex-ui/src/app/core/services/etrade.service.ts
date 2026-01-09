import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface EtradeAccount {
  id: string;
  userId: string;
  accountIdKey: string;
  accountType: string;
  accountName: string;
  accountStatus: string;
  linkedAt: string;
  lastSyncedAt: string | null;
}

export interface OAuthStatus {
  connected: boolean;
  hasAccounts: boolean;
  accountCount: number;
  tokenStatus?: 'VALID' | 'EXPIRED' | 'MISSING' | 'INVALID';
  tokenExpiresAt?: string;
}

export interface TokenStatus {
  valid: boolean;
  expiresAt?: string;
  accountId?: string;
}

export interface OAuthInitiateResponse {
  authorizationUrl: string;
  state: string;
  requestToken?: string;
  correlationId?: string;
  authAttemptId?: string;
}

@Injectable({ providedIn: 'root' })
export class EtradeService {
  private readonly baseUrl = `${environment.apiUrl}/etrade`;

  constructor(private http: HttpClient) {}

  /**
   * Gets OAuth status for the current user.
   */
  getOAuthStatus(userId?: string): Observable<OAuthStatus> {
    console.log('[EtradeService] getOAuthStatus - userId:', userId);
    let params = new HttpParams();
    if (userId) {
      params = params.set('userId', userId);
    }
    const url = `${this.baseUrl}/oauth/status`;
    console.log('[EtradeService] Calling:', url, 'params:', params.toString());
    return this.http.get<OAuthStatus>(url, { params }).pipe(
      tap(status => console.log('[EtradeService] getOAuthStatus - response received:', status))
    );
  }

  /**
   * Gets token status for a specific account by attempting to validate it.
   * This is done by trying to call List Accounts API.
   */
  getTokenStatus(accountId: string): Observable<TokenStatus> {
    // Try to validate by calling accounts API
    return new Observable(observer => {
      this.getAccounts().subscribe({
        next: accounts => {
          // If we get accounts, token is valid
          observer.next({ valid: true });
          observer.complete();
        },
        error: err => {
          // If 401/403, token is invalid/expired
          if (err.status === 401 || err.status === 403) {
            observer.next({ valid: false });
          } else {
            // Other errors - assume token might be valid but something else failed
            observer.next({ valid: true });
          }
          observer.complete();
        }
      });
    });
  }

  /**
   * Initiates OAuth flow - Step 1: Get request token and authorization URL.
   */
  initiateOAuth(userId?: string, correlationId?: string): Observable<OAuthInitiateResponse> {
    console.log('[EtradeService] initiateOAuth - userId:', userId, 'correlationId:', correlationId);
    let params = new HttpParams();
    if (userId) {
      params = params.set('userId', userId);
    }
    if (correlationId) {
      params = params.set('correlationId', correlationId);
    }
    const url = `${this.baseUrl}/oauth/authorize`;
    console.log('[EtradeService] Calling:', url, 'params:', params.toString());
    return this.http.get<OAuthInitiateResponse>(url, { params }).pipe(
      tap(response => console.log('[EtradeService] initiateOAuth - response received:', response))
    );
  }

  /**
   * Validates token by calling List Accounts API.
   */
  validateToken(accountId: string): Observable<EtradeAccount[]> {
    return this.http.get<EtradeAccount[]>(`${this.baseUrl}/accounts`, {
      params: new HttpParams().set('accountId', accountId)
    });
  }

  /**
   * Gets all linked accounts for a user.
   */
  getAccounts(userId?: string): Observable<EtradeAccount[]> {
    console.log('[EtradeService] getAccounts - userId:', userId);
    let params = new HttpParams();
    if (userId) {
      params = params.set('userId', userId);
    }
    const url = `${this.baseUrl}/accounts`;
    console.log('[EtradeService] Calling:', url, 'params:', params.toString());
    return this.http.get<EtradeAccount[]>(url, { params }).pipe(
      tap(accounts => console.log('[EtradeService] getAccounts - response received:', accounts))
    );
  }

  /**
   * Gets account details by ID.
   */
  getAccount(accountId: string): Observable<EtradeAccount> {
    return this.http.get<EtradeAccount>(`${this.baseUrl}/accounts/${accountId}`);
  }

  /**
   * Syncs accounts from E*TRADE.
   */
  syncAccounts(accountId: string, userId?: string): Observable<EtradeAccount[]> {
    console.log('[EtradeService] syncAccounts - accountId:', accountId, 'userId:', userId);
    let params = new HttpParams().set('accountId', accountId);
    if (userId) {
      params = params.set('userId', userId);
    }
    const url = `${this.baseUrl}/accounts/sync`;
    console.log('[EtradeService] Calling:', url, 'params:', params.toString());
    return this.http.post<EtradeAccount[]>(url, null, { params }).pipe(
      tap(accounts => console.log('[EtradeService] syncAccounts - response received:', accounts))
    );
  }

  /**
   * Connects/validates an account by checking token and syncing if needed.
   */
  connectAccount(accountId: string, userId?: string): Observable<EtradeAccount[]> {
    // Try to sync accounts - this will validate the token
    return this.syncAccounts(accountId, userId);
  }

  /**
   * Renews access token for an account.
   */
  renewToken(accountId: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.baseUrl}/oauth/renew-token`,
      null,
      { params: new HttpParams().set('accountId', accountId) }
    );
  }

  /**
   * Revokes access token for an account.
   */
  revokeToken(accountId: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.baseUrl}/oauth/revoke-token`,
      null,
      { params: new HttpParams().set('accountId', accountId) }
    );
  }
}
