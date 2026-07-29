import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageDetail } from './message-detail';
import { MessageService } from '../../../core/services/message.service';
import { MessageResponse } from '../../../core/models/message.model';

const sample: MessageResponse = {
  id: '1',
  mqMessageId: 'mq-1',
  correlationId: 'corr-1',
  sourceQueue: 'DEV.QUEUE.1',
  sourceApplication: 'BACKOFFICE',
  status: 'PROCESSED',
  headers: { JMSXAppID: 'amqsput' },
  payload: '<payload/>',
  receivedAt: '2026-07-27T09:00:00Z',
  processedAt: '2026-07-27T09:00:01Z',
};

describe('MessageDetail', () => {
  it('loads and displays the message for the given id', () => {
    const messageService = { getById: () => of(sample) } as unknown as MessageService;

    TestBed.configureTestingModule({
      imports: [MessageDetail],
      providers: [provideRouter([]), { provide: MessageService, useValue: messageService }],
    });

    const fixture = TestBed.createComponent(MessageDetail);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    expect(fixture.componentInstance['message']()).toEqual(sample);
  });

  it('shows a not-found message on 404', () => {
    const messageService = {
      getById: () => throwError(() => ({ status: 404 })),
    } as unknown as MessageService;

    TestBed.configureTestingModule({
      imports: [MessageDetail],
      providers: [provideRouter([]), { provide: MessageService, useValue: messageService }],
    });

    const fixture = TestBed.createComponent(MessageDetail);
    fixture.componentRef.setInput('id', 'missing');
    fixture.detectChanges();

    expect(fixture.componentInstance['error']()).toContain('introuvable');
  });
});
