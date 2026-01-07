# Stock Quote Review Feature

This document describes the Stock Quote Review feature implementation.

## Overview

The Stock Quote Review feature allows users to:
- Search for stock quotes by symbol
- View real-time quotes and price history
- Visualize data with interactive charts (line or candlestick)
- Review and annotate quotes
- Persist all searches to the database
- View and manage search history from the Review Stack dashboard

## Architecture

### Backend (Spring Boot)

**Entities:**
- `StockQuoteSearch` - Domain entity for persisted searches
- Enums: `QuoteRange`, `SearchStatus`, `ReviewStatus`

**Repositories:**
- `StockQuoteSearchRepository` - JPA repository with filtering support

**Services:**
- `StockQuoteSearchService` - Business logic for search persistence and management
- `MarketDataService` - Fetches quotes from providers (Alpha Vantage, etc.)

**Controllers:**
- `StockQuoteController` - REST API endpoints

**Database:**
- Table: `stock_quote_search` (Liquibase migration 0003)
- Indexes on symbol, created_at, status, and user_id

### Frontend (Angular)

**Components:**
- `StockReviewComponent` - Main review page
- `StockReviewStackComponent` - Dashboard for viewing all searches
- `StockChartComponent` - Chart visualization using ApexCharts

**Services:**
- `StockQuoteService` - HTTP client for API calls with caching

**Models:**
- `StockQuote`, `StockQuoteHistory`, `StockQuoteSearch`, `StockReview`

## API Endpoints

### Quote Retrieval
```
GET /api/stock/quotes/{symbol}?source=alphavantage
GET /api/stock/quotes/{symbol}/history?range=1D&source=alphavantage
POST /api/stock/quotes/{symbol}/search?range=1D&source=alphavantage
```

### Search Management
```
GET /api/stock/searches?symbol=AAPL&status=SUCCESS&page=0&size=20
GET /api/stock/searches/{id}
POST /api/stock/searches/{id}/rerun?userId={uuid}
PUT /api/stock/searches/{id}/review
```

## Features

### 1. Quote Search
- Symbol validation (letters only, max 10 chars)
- Time range selection (1D, 5D, 1M, 3M, 1Y)
- Optional exchange field
- Automatic symbol normalization (uppercase)

### 2. Quote Display
- **Summary Cards**: Last price, change, day range, volume, open, high, low, updated time
- **Price Chart**: Line or candlestick visualization
- Color-coded price changes (green for positive, red for negative)

### 3. Review Panel
- Review status: Not Reviewed / Reviewed
- Free-text notes
- Persist to database

### 4. Review Stack Dashboard
- Table view of all searches
- Filtering by symbol and status
- Pagination support
- Re-run searches directly from dashboard
- View search details

### 5. Caching
- Client-side caching (60 seconds TTL)
- Reduces API calls and improves performance
- Automatic cache invalidation on new searches

## Routes

- `/stock-review` - Main review page
- `/stock-review/searches` - Review Stack dashboard

## Configuration

### Backend
No additional configuration required. Uses existing Alpha Vantage setup.

### Frontend
Ensure ApexCharts is installed:
```bash
npm install apexcharts ng-apexcharts
```

## Environment Variables

None required for MVP. The feature uses existing backend API configuration.

## Testing

### Unit Tests (To Be Implemented)

**Backend:**
- `StockQuoteSearchServiceTest` - Service logic, search creation, filtering
- `StockQuoteControllerTest` - API endpoints, request/response mapping
- `StockQuoteSearchRepositoryTest` - Database queries, filtering

**Frontend:**
- `StockQuoteServiceTest` - HTTP mocking, caching behavior
- `StockReviewComponentTest` - Component rendering, form validation
- `StockReviewStackComponentTest` - Dashboard functionality
- `StockChartComponentTest` - Chart rendering, data transformation

### E2E Tests (To Be Implemented)

Using Playwright or Cypress:
1. Navigate to Stock Quote Review page
2. Enter symbol (AAPL) and fetch quote
3. Verify quote fields render
4. Verify chart renders
5. Add review note and save
6. Navigate to Review Stack
7. Verify search appears in dashboard
8. Re-run search from dashboard
9. Verify new search is created

## Usage

### Basic Workflow

1. Navigate to "Stock Quote Review" from sidebar
2. Enter a symbol (e.g., AAPL)
3. Select time range (e.g., 1D)
4. Click "Fetch Quote"
5. Review quote data and chart
6. Add review notes if desired
7. Mark as "Reviewed" and save

### Review Stack Workflow

1. Navigate to "Review Stack" from sidebar
2. Filter searches by symbol or status
3. View search details
4. Click "Re-run" to fetch fresh data
5. Click "View" to see full review

## Limitations & Future Enhancements

### Current Limitations
- Historical data currently returns single current quote (MVP limitation)
- Real-time updates require manual refresh
- No user authentication (userId is optional)
- Chart library requires npm installation

### Future Enhancements
- Real-time updates via WebSocket/SSE
- Full historical time-series data
- User authentication integration
- Export reviews to PDF/CSV
- Advanced charting options (technical indicators)
- Multi-symbol comparison
- Watchlist functionality

## Dependencies

### Frontend
- `apexcharts@^3.44.0` - Charting library
- `ng-apexcharts@^1.7.1` - Angular wrapper for ApexCharts

### Backend
- Uses existing dependencies (Spring Boot, JPA, Liquibase)

## Troubleshooting

### Chart Not Rendering
- Verify ApexCharts is installed: `npm install apexcharts ng-apexcharts`
- Check browser console for errors
- Ensure data array is not empty

### Search Not Persisting
- Check backend logs for errors
- Verify database connection
- Check Liquibase migration ran successfully

### API Errors
- Verify backend is running on port 8080
- Check CORS configuration if accessing from different origin
- Verify Alpha Vantage API key is configured

## Security Notes

- API keys should be proxied through backend in production
- User input is validated and sanitized
- Database queries use parameterized statements (JPA)
- XSS protection via Angular's built-in sanitization
