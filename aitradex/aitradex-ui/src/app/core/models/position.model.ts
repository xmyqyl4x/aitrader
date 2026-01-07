export interface Position {
  id: string;
  accountId: string;
  symbol: string;
  quantity: number;
  costBasis: number;
  stopLoss?: number;
  openedAt: string;
  closedAt?: string;
}
