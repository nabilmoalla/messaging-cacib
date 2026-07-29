import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'messages' },
  {
    path: 'messages',
    loadComponent: () => import('./features/messages/message-list/message-list').then((m) => m.MessageList),
  },
  {
    path: 'messages/:id',
    loadComponent: () =>
      import('./features/messages/message-detail/message-detail').then((m) => m.MessageDetail),
  },
];
