export interface Upload {
  id: string;
  userId: string;
  type: 'CSV' | 'JSON' | 'EXCEL';
  status: string;
  fileName: string;
  storedPath?: string;
  parsedRowCount?: number;
  errorReport?: string;
  createdAt?: string;
  completedAt?: string;
}
