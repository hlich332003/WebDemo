import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  showNotification(
    message: string,
    type: 'success' | 'error' | 'info' | 'warning' = 'info',
  ): void {
    const notification = document.getElementById('notification');
    if (!notification) return;

    notification.textContent = message;
    notification.className = `notification ${type}`;

    setTimeout(() => notification.classList.add('show'), 100);
    setTimeout(() => notification.classList.remove('show'), 3000);
  }

  success(message: string): void {
    this.showNotification(message, 'success');
  }

  error(message: string): void {
    this.showNotification(message, 'error');
  }

  info(message: string): void {
    this.showNotification(message, 'info');
  }

  warning(message: string): void {
    this.showNotification(message, 'warning');
  }
}
