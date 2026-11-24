import { request } from './client';

export type ImportStatus = 'IN_PROGRESS' | 'SUCCESS' | 'FAILED';

interface ImportOperation {
  id: number;
  username: string;
  status: ImportStatus;
  addedCount?: number;
}

interface ImportPage {
  items: ImportOperation[];
  pageSize: number;
  hasMore: boolean;
}

interface ImportResult {
  id: number;
  addedCount: number;
}

export function uploadImport(file: File): Promise<ImportResult> {
  const body = new FormData();
  body.append('file', file);
  return request<ImportResult>('POST', '/imports', { body });
}

export function listImports(offset: number, signal: AbortSignal): Promise<ImportPage> {
  return request<ImportPage>('GET', '/imports', { query: { offset }, signal });
}
