import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { EMPTY_MESSAGE_FILTER } from '../models/message.model';
import { MessageService } from './message.service';

describe('MessageService', () => {
  let service: MessageService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/v1/messages`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MessageService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('requests an offset page with page/size params', () => {
    service.listByOffset(EMPTY_MESSAGE_FILTER, 1, 25).subscribe();

    const req = httpMock.expectOne((r) => r.url === baseUrl);
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('25');
    req.flush({ content: [], page: 1, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('includes filter params only when set', () => {
    service
      .listByOffset({ status: 'ERROR', sourceQueue: 'DEV.QUEUE.1', from: null, to: null }, 0, 50)
      .subscribe();

    const req = httpMock.expectOne((r) => r.url === baseUrl);
    expect(req.request.params.get('status')).toBe('ERROR');
    expect(req.request.params.get('sourceQueue')).toBe('DEV.QUEUE.1');
    expect(req.request.params.has('from')).toBe(false);
    req.flush({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
  });

  it('requests a single message by id', () => {
    service.getById('123').subscribe();

    const req = httpMock.expectOne(`${baseUrl}/123`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('requests stats', () => {
    service.getStats().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/stats`);
    req.flush({ byStatus: {}, bySourceQueue: {} });
  });
});
