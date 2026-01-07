import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  StockQuote,
  StockQuoteHistory,
  StockQuoteSearch,
  QuoteRange
} from '../models/stock-quote.model';

interface CacheEntry<T> {
  data: T;
  expiresAt: number;
}

@Injectable({
  providedIn: 'root'
})
export class StockQuoteService {
  private readonly baseUrl = `${environment.apiUrl}/stock`;
  private quoteCache = new Map<string, CacheEntry<StockQuote>>();
  private historyCache = new Map<string, CacheEntry<StockQuoteHistory[]>>();
  private readonly CACHE_TTL_MS = 60000; // 60 seconds

  constructor(private http: HttpClient) {}

  getQuote(symbol: string, source?: string): Observable<StockQuote> {
    const cacheKey = `quote:${symbol}:${source || 'default'}`;
    const cached = this.quoteCache.get(cacheKey);
    
    if (cached && Date.now() < cached.expiresAt) {
      return of(cached.data);
    }

    let params = new HttpParams();
    if (source) {
      params = params.set('source', source);
    }

    return this.http.get<StockQuote>(`${this.baseUrl}/quotes/${symbol}`, { params }).pipe(
      tap(quote => {
        this.quoteCache.set(cacheKey, {
          data: quote,
          expiresAt: Date.now() + this.CACHE_TTL_MS
        });
      }),
      catchError(error => {
        console.error('Error fetching quote:', error);
        throw error;
      })
    );
  }

  getHistory(symbol: string, range: QuoteRange, source?: string): Observable<StockQuoteHistory[]> {
    const cacheKey = `history:${symbol}:${range}:${source || 'default'}`;
    const cached = this.historyCache.get(cacheKey);
    
    if (cached && Date.now() < cached.expiresAt) {
      return of(cached.data);
    }

    let params = new HttpParams()
      .set('range', range);
    if (source) {
      params = params.set('source', source);
    }

    return this.http.get<StockQuoteHistory[]>(`${this.baseUrl}/quotes/${symbol}/history`, { params }).pipe(
      tap(history => {
        this.historyCache.set(cacheKey, {
          data: history,
          expiresAt: Date.now() + this.CACHE_TTL_MS
        });
      }),
      catchError(error => {
        console.error('Error fetching history:', error);
        throw error;
      })
    );
  }

  searchAndSave(symbol: string, range: QuoteRange, source?: string, userId?: string): Observable<StockQuoteSearch> {
    let params = new HttpParams().set('range', range);
    if (source) {
      params = params.set('source', source);
    }
    if (userId) {
      params = params.set('userId', userId);
    }

    // Clear cache for this symbol after search
    this.clearCacheForSymbol(symbol);

    return this.http.post<StockQuoteSearch>(
      `${this.baseUrl}/quotes/${symbol}/search`,
      null,
      { params }
    );
  }

  listSearches(
    page: number = 0,
    size: number = 20,
    symbol?: string,
    status?: string,
    dateFrom?: string,
    dateTo?: string
  ): Observable<{ content: StockQuoteSearch[]; totalElements: number; totalPages: number; size: number; number: number }> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (symbol) {
      params = params.set('symbol', symbol);
    }
    if (status) {
      params = params.set('status', status);
    }
    if (dateFrom) {
      params = params.set('dateFrom', dateFrom);
    }
    if (dateTo) {
      params = params.set('dateTo', dateTo);
    }

    return this.http.get<any>(`${this.baseUrl}/searches`, { params }).pipe(
      map(response => ({
        content: response.content || [],
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
        size: response.size || 0,
        number: response.number || 0
      }))
    );
  }

  getSearch(id: string): Observable<StockQuoteSearch> {
    return this.http.get<StockQuoteSearch>(`${this.baseUrl}/searches/${id}`);
  }

  rerunSearch(id: string, userId?: string): Observable<StockQuoteSearch> {
    let params = new HttpParams();
    if (userId) {
      params = params.set('userId', userId);
    }
    return this.http.post<StockQuoteSearch>(`${this.baseUrl}/searches/${id}/rerun`, null, { params });
  }

  updateReview(id: string, reviewStatus: string, reviewNote: string | null): Observable<StockQuoteSearch> {
    return this.http.put<StockQuoteSearch>(`${this.baseUrl}/searches/${id}/review`, {
      reviewStatus,
      reviewNote
    });
  }

  private clearCacheForSymbol(symbol: string): void {
    const keysToDelete: string[] = [];
    this.quoteCache.forEach((_, key) => {
      if (key.includes(`:${symbol}:`)) {
        keysToDelete.push(key);
      }
    });
    this.historyCache.forEach((_, key) => {
      if (key.includes(`:${symbol}:`)) {
        keysToDelete.push(key);
      }
    });
    keysToDelete.forEach(key => {
      this.quoteCache.delete(key);
      this.historyCache.delete(key);
    });
  }

  clearCache(): void {
    this.quoteCache.clear();
    this.historyCache.clear();
  }
}
