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

  // ✅ Lấy tham số từ URL
  getUrlParam(name: string): string | null {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
  }
}
