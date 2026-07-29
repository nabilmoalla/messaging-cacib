import { Component, computed, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { MessageStatus } from '../../core/models/message.model';

const STATUS_LABELS: Record<MessageStatus, string> = {
  RECEIVED: 'Reçu',
  PROCESSED: 'Traité',
  ERROR: 'Erreur',
};

@Component({
  selector: 'app-status-chip',
  imports: [MatChipsModule],
  templateUrl: './status-chip.html',
  styleUrl: './status-chip.scss',
})
export class StatusChip {
  readonly status = input.required<MessageStatus>();
  protected readonly label = computed(() => STATUS_LABELS[this.status()]);
}
