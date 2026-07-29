import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MessageService } from '../../../core/services/message.service';
import {
  MessageFilter,
  MessageResponse,
  MessageStatus,
} from '../../../core/models/message.model';
import { StatusChip } from '../../../shared/status-chip/status-chip';

const DISPLAYED_COLUMNS = [
  'receivedAt',
  'sourceQueue',
  'sourceApplication',
  'correlationId',
  'status',
];

@Component({
  selector: 'app-message-list',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatIconModule,
    StatusChip,
  ],
  templateUrl: './message-list.html',
  styleUrl: './message-list.scss',
})
export class MessageList implements OnInit {
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);

  protected readonly displayedColumns = DISPLAYED_COLUMNS;
  protected readonly statuses: MessageStatus[] = ['RECEIVED', 'PROCESSED', 'ERROR'];

  protected readonly messages = signal<MessageResponse[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly page = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected status: MessageStatus | null = null;
  protected sourceQueue = '';
  protected from = '';
  protected to = '';

  ngOnInit(): void {
    this.load();
  }

  protected onPage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  protected applyFilters(): void {
    this.page.set(0);
    this.load();
  }

  protected resetFilters(): void {
    this.status = null;
    this.sourceQueue = '';
    this.from = '';
    this.to = '';
    this.page.set(0);
    this.load();
  }

  protected openDetail(message: MessageResponse): void {
    this.router.navigate(['/messages', message.id]);
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);

    const filter = this.currentFilter();
    this.messageService.listByOffset(filter, this.page(), this.pageSize()).subscribe({
      next: (result) => {
        this.messages.set(result.content);
        this.totalElements.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les messages. Vérifiez que le backend est démarré.');
        this.loading.set(false);
      },
    });
  }

  private currentFilter(): MessageFilter {
    return {
      status: this.status,
      sourceQueue: this.sourceQueue || null,
      from: this.from ? new Date(this.from).toISOString() : null,
      to: this.to ? new Date(this.to).toISOString() : null,
    };
  }
}
