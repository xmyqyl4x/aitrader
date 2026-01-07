export interface Order {
  id: string;
  accountId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  type: 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT';
  status: string;
  source: string;
  limitPrice?: number;
  stopPrice?: number;
  quantity: number;
  routedAt?: string;
  filledAt?: string;
  notes?: string;
  createdAt?: string;
}
