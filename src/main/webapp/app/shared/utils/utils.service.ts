import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class UtilsService {
  constructor() {}

  // ✅ Định dạng giá tiền
  formatPrice(price: string | number): string {
    if (typeof price === 'string') {
      price = parseInt(price.replace(/\./g, ''));
    }
    return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.') + 'đ';
  }

  // ✅ Hiển thị thông báo
  showNotification(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    const notification = document.getElementById('notification');
    if (!notification) return;

    notification.textContent = message;
    notification.className = `notification ${type}`;

    setTimeout(() => {
      notification.classList.remove('hidden');
    }, 100);

    setTimeout(() => {
      notification.classList.add('hidden');
    }, 3000);
  }

  // ✅ Lấy tham số từ URL
  getUrlParam(name: string): string | null {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
  }
}
