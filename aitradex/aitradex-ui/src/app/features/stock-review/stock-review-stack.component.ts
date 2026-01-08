import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { StockQuoteService } from '../../core/services/stock-quote.service';
import { StockQuoteSearch } from '../../core/models/stock-quote.model';

@Component({
  selector: 'app-stock-review-stack',
  templateUrl: './stock-review-stack.component.html',
  styleUrls: ['./stock-review-stack.component.scss']
})
export class StockReviewStackComponent implements OnInit {
  searches: StockQuoteSearch[] = [];
  loading = false;
  error: string | null = null;
  
  filterForm: FormGroup;
  page = 0;
  size = 20;
  totalElements = 0;
  totalPages = 0;

  constructor(
    private stockQuoteService: StockQuoteService,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.filterForm = this.fb.group({
      symbol: [''],
      status: ['']
    });
  }

  ngOnInit() {
    this.loadSearches();
  }

  loadSearches() {
    this.loading = true;
    this.error = null;

    const filters = this.filterForm.value;

    this.stockQuoteService.listSearches(
      this.page,
      this.size,
      filters.symbol || undefined,
      filters.status || undefined
    ).subscribe({
      next: response => {
        this.searches = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: err => {
        this.handleError(err);
        this.loading = false;
      }
    });
  }

  onFilter() {
    this.page = 0;
    this.loadSearches();
  }

  onRerun(search: StockQuoteSearch) {
    this.loading = true;
    this.error = null;

    this.stockQuoteService.rerunSearch(search.id).subscribe({
      next: newSearch => {
        // Reload searches
        this.loadSearches();
        // Navigate to review page with the new search
        this.router.navigate(['/stock-review'], {
          queryParams: { symbol: newSearch.symbol, searchId: newSearch.id }
        });
      },
      error: err => {
        this.handleError(err);
        this.loading = false;
      }
    });
  }

  onView(search: StockQuoteSearch) {
    this.router.navigate(['/stock-review'], {
      queryParams: { symbol: search.symbol, searchId: search.id }
    });
  }

  onPageChange(page: number) {
    this.page = page;
    this.loadSearches();
  }

  getStatusClass(status: string): string {
    return status === 'SUCCESS' ? 'status-success' : 'status-failed';
  }

  getReviewStatusClass(status: string): string {
    return status === 'REVIEWED' ? 'review-reviewed' : 'review-not-reviewed';
  }

  // Expose Math for template
  Math = Math;

  private handleError(error: any) {
    console.error('Error:', error);
    this.error = error.message || 'An error occurred while loading searches.';
  }
}
