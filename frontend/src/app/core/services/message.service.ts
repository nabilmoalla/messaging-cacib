import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  MessageFilter,
  MessagePageResponse,
  MessageResponse,
  MessageStatsResponse,
} from '../models/message.model';

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/messages`;

  listByOffset(filter: MessageFilter, page: number, size: number): Observable<MessagePageResponse> {
    const params = this.buildFilterParams(filter).set('page', page).set('size', size);
    return this.http.get<MessagePageResponse>(this.baseUrl, { params });
  }

  getById(id: string): Observable<MessageResponse> {
    return this.http.get<MessageResponse>(`${this.baseUrl}/${id}`);
  }

  getStats(): Observable<MessageStatsResponse> {
    return this.http.get<MessageStatsResponse>(`${this.baseUrl}/stats`);
  }

  private buildFilterParams(filter: MessageFilter): HttpParams {
    let params = new HttpParams();
    if (filter.status) params = params.set('status', filter.status);
    if (filter.sourceQueue) params = params.set('sourceQueue', filter.sourceQueue);
    if (filter.from) params = params.set('from', filter.from);
    if (filter.to) params = params.set('to', filter.to);
    return params;
  }
}
