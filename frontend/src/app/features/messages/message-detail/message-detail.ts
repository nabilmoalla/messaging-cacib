import { Component, computed, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MessageService } from '../../../core/services/message.service';
import { MessageResponse } from '../../../core/models/message.model';
import { StatusChip } from '../../../shared/status-chip/status-chip';

interface DetailState {
  loading: boolean;
  message: MessageResponse | null;
  error: string | null;
}

const LOADING_STATE: DetailState = { loading: true, message: null, error: null };

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

  // switchMap cancels the previous in-flight getById() call whenever id() changes, so
  // navigating A -> B quickly can never let A's slower response overwrite B's on screen.
  private readonly state = toSignal(
    toObservable(this.id).pipe(
      switchMap((id) =>
        this.messageService.getById(id).pipe(
          map((message): DetailState => ({ loading: false, message, error: null })),
          catchError((err) =>
            of<DetailState>({
              loading: false,
              message: null,
              error: err?.status === 404 ? 'Ce message est introuvable.' : 'Impossible de charger le message.',
            })
          ),
          startWith(LOADING_STATE)
        )
      )
    ),
    { initialValue: LOADING_STATE }
  );

  protected readonly message = computed(() => this.state().message);
  protected readonly loading = computed(() => this.state().loading);
  protected readonly error = computed(() => this.state().error);

  protected headerEntries(): [string, string][] {
    const headers = this.message()?.headers ?? {};
    return Object.entries(headers);
  }
}
