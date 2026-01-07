export interface AnalyticsSummary {
  accountId: string;
  startDate: string;
  endDate: string;
  startingEquity: number;
  endingEquity: number;
  absolutePnl: number;
  returnPct: number;
  maxDrawdown: number;
}

export interface EquityPoint {
  asOfDate: string;
  equity: number;
  drawdown?: number;
}

export interface SymbolPnl {
  symbol: string;
  quantity: number;
  costBasis: number;
  lastPrice: number;
  unrealizedPnl: number;
}
