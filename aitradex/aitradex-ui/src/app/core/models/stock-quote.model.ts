export interface StockQuote {
  symbol: string;
  asOf: string;
  open: number | null;
  high: number | null;
  low: number | null;
  close: number | null;
  volume: number | null;
  source: string;
}

export interface StockQuoteHistory {
  timestamp: string;
  open: number | null;
  high: number | null;
  low: number | null;
  close: number | null;
  volume: number | null;
}

export interface StockQuoteSearch {
  id: string;
  createdAt: string;
  createdByUserId: string | null;
  symbol: string;
  companyName: string | null;
  exchange: string | null;
  range: string;
  requestedAt: string;
  status: 'SUCCESS' | 'FAILED';
  quoteTimestamp: string | null;
  price: number | null;
  currency: string | null;
  changeAmount: number | null;
  changePercent: number | null;
  volume: number | null;
  provider: string;
  requestId: string | null;
  correlationId: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  durationMs: number | null;
  reviewStatus: 'NOT_REVIEWED' | 'REVIEWED';
  reviewNote: string | null;
  reviewedAt: string | null;
}

export interface StockReview {
  reviewStatus: 'NOT_REVIEWED' | 'REVIEWED';
  reviewNote: string | null;
  rating?: 'Bullish' | 'Neutral' | 'Bearish';
}

export type QuoteRange = '1D' | '5D' | '1M' | '3M' | '1Y';
