import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { StockQuoteService } from '../../core/services/stock-quote.service';
import { StockQuote, StockQuoteHistory, QuoteRange, StockReview } from '../../core/models/stock-quote.model';

@Component({
  selector: 'app-stock-review',
  templateUrl: './stock-review.component.html',
  styleUrls: ['./stock-review.component.css']
})
export class StockReviewComponent implements OnInit {
  searchForm: FormGroup;
  quote: StockQuote | null = null;
  history: StockQuoteHistory[] = [];
  review: StockReview = {
    reviewStatus: 'NOT_REVIEWED',
    reviewNote: null
  };
  searchId: string | null = null;
  
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  ranges: QuoteRange[] = ['1D', '5D', '1M', '3M', '1Y'];
  chartType: 'line' | 'candlestick' = 'line';

  constructor(
    private fb: FormBuilder,
    private stockQuoteService: StockQuoteService
  ) {
    this.searchForm = this.fb.group({
      symbol: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{1,10}$/)]],
      range: ['1D', Validators.required],
      exchange: ['']
    });
  }

  ngOnInit() {
    // Try to load from localStorage if available
    const savedSymbol = localStorage.getItem('lastStockSymbol');
    if (savedSymbol) {
      this.searchForm.patchValue({ symbol: savedSymbol });
    }
  }

  onSearch() {
    if (this.searchForm.invalid) {
      return;
    }

    const symbol = this.searchForm.value.symbol.toUpperCase().trim();
    const range = this.searchForm.value.range;
    const exchange = this.searchForm.value.exchange;

    this.loading = true;
    this.error = null;
    this.successMessage = null;
    this.quote = null;
    this.history = [];
    this.review = {
      reviewStatus: 'NOT_REVIEWED',
      reviewNote: null
    };

    // Save symbol to localStorage
    localStorage.setItem('lastStockSymbol', symbol);

    // Fetch quote and history, then save search
    this.stockQuoteService.getQuote(symbol).subscribe({
      next: quote => {
        this.quote = quote;
        
        // Fetch history
        this.stockQuoteService.getHistory(symbol, range).subscribe({
          next: history => {
            this.history = history;
            
            // Save search to backend
            this.stockQuoteService.searchAndSave(symbol, range).subscribe({
              next: search => {
                this.searchId = search.id;
                this.review = {
                  reviewStatus: search.reviewStatus,
                  reviewNote: search.reviewNote || null
                };
                this.loading = false;
                this.successMessage = 'Quote retrieved successfully';
              },
              error: err => {
                console.error('Error saving search:', err);
                this.loading = false;
                // Don't fail the whole operation if save fails
                this.successMessage = 'Quote retrieved (search not saved)';
              }
            });
          },
          error: err => {
            this.handleError(err);
            this.loading = false;
          }
        });
      },
      error: err => {
        this.handleError(err);
        this.loading = false;
      }
    });
  }

  onSaveReview() {
    if (!this.searchId) {
      this.error = 'No search ID available. Please search for a quote first.';
      return;
    }

    this.stockQuoteService.updateReview(
      this.searchId,
      this.review.reviewStatus,
      this.review.reviewNote
    ).subscribe({
      next: search => {
        this.review = {
          reviewStatus: search.reviewStatus,
          reviewNote: search.reviewNote || null
        };
        this.successMessage = 'Review saved successfully';
        this.error = null;
      },
      error: err => {
        this.handleError(err);
      }
    });
  }

  getChangeClass(): string {
    if (!this.quote || !this.quote.close) return '';
    // For simplicity, assume positive if close > open
    if (this.quote.open && this.quote.close > this.quote.open) {
      return 'positive';
    }
    return 'negative';
  }

  getChangePercent(): number | null {
    if (!this.quote || !this.quote.close || !this.quote.open) return null;
    const change = this.quote.close - this.quote.open;
    return (change / this.quote.open) * 100;
  }

  toggleChartType() {
    this.chartType = this.chartType === 'line' ? 'candlestick' : 'line';
  }

  private handleError(error: any) {
    console.error('Error:', error);
    if (error.status === 404) {
      this.error = 'Stock symbol not found. Please check the symbol and try again.';
    } else if (error.status === 429) {
      this.error = 'Rate limit exceeded. Please wait a moment and try again.';
    } else if (error.status === 0) {
      this.error = 'Network error. Please check your connection.';
    } else {
      this.error = error.message || 'An error occurred while fetching the quote.';
    }
  }
}
