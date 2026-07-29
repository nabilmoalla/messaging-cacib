import { Component, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MessageService } from '../../../core/services/message.service';
import { MessageResponse } from '../../../core/models/message.model';
import { StatusChip } from '../../../shared/status-chip/status-chip';

@Component({
  selector: 'app-message-detail',
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    StatusChip,
  ],
  templateUrl: './message-detail.html',
  styleUrl: './message-detail.scss',
})
export class MessageDetail {
  private readonly messageService = inject(MessageService);

  readonly id = input.required<string>();

  protected readonly message = signal<MessageResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const id = this.id();
      this.loading.set(true);
      this.error.set(null);

      this.messageService.getById(id).subscribe({
        next: (message) => {
          this.message.set(message);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(
            err?.status === 404 ? 'Ce message est introuvable.' : 'Impossible de charger le message.'
          );
          this.loading.set(false);
        },
      });
    });
  }

  protected headerEntries(): [string, string][] {
    const headers = this.message()?.headers ?? {};
    return Object.entries(headers);
  }
}
