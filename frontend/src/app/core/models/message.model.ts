export type MessageStatus = 'RECEIVED' | 'PROCESSED' | 'ERROR';

export interface MessageResponse {
  id: string;
  mqMessageId: string;
  correlationId: string | null;
  sourceQueue: string | null;
  sourceApplication: string | null;
  status: MessageStatus;
  headers: Record<string, string>;
  payload: string | null;
  receivedAt: string;
  processedAt: string | null;
}

export interface MessagePageResponse {
  content: MessageResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MessageStatsResponse {
  byStatus: Record<string, number>;
  bySourceQueue: Record<string, number>;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface MessageFilter {
  status: MessageStatus | null;
  sourceQueue: string | null;
  from: string | null;
  to: string | null;
}

export const EMPTY_MESSAGE_FILTER: MessageFilter = {
  status: null,
  sourceQueue: null,
  from: null,
  to: null,
};
