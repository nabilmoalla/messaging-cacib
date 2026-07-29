import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MessageList } from './message-list';
import { MessageService } from '../../../core/services/message.service';
import { MessagePageResponse, MessageResponse } from '../../../core/models/message.model';

function sampleMessage(id: string): MessageResponse {
  return {
    id,
    mqMessageId: 'mq-1',
    correlationId: 'corr-1',
    sourceQueue: 'DEV.QUEUE.1',
    sourceApplication: 'BACKOFFICE',
    status: 'PROCESSED',
    headers: {},
    payload: '<payload/>',
    receivedAt: '2026-07-27T09:00:00Z',
    processedAt: '2026-07-27T09:00:01Z',
  };
}

describe('MessageList', () => {
  it('loads the first page of messages on init', () => {
    const page: MessagePageResponse = {
      content: [sampleMessage('1')],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    };
    const messageService = {
      listByOffset: () => of(page),
      getStats: () => of({ byStatus: { RECEIVED: 1 }, bySourceQueue: {} }),
    } as unknown as MessageService;

    TestBed.configureTestingModule({
      imports: [MessageList],
      providers: [provideRouter([]), { provide: MessageService, useValue: messageService }],
    });

    const fixture = TestBed.createComponent(MessageList);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component['messages']()).toHaveLength(1);
    expect(component['totalElements']()).toBe(1);
    expect(component['totalMessages']()).toBe(1);
  });

  it('surfaces an error message when the request fails', () => {
    const messageService = {
      listByOffset: () => ({ subscribe: (observer: any) => observer.error({ status: 500 }) }),
      getStats: () => of({ byStatus: {}, bySourceQueue: {} }),
    } as unknown as MessageService;

    TestBed.configureTestingModule({
      imports: [MessageList],
      providers: [provideRouter([]), { provide: MessageService, useValue: messageService }],
    });

    const fixture = TestBed.createComponent(MessageList);
    fixture.detectChanges();

    expect(fixture.componentInstance['error']()).toContain('backend');
  });
});
