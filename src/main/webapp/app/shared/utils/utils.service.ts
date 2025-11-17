import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class UtilsService {
  constructor() {}

  // ✅ Định dạng giá tiền
  formatPrice(price: string | number): string {
    let numPrice: number;
    if (typeof price === 'string') {
      numPrice = parseFloat(price);
    } else {
      numPrice = price;
    }
    return (
      Math.round(numPrice)
        .toString()
        .replace(/\B(?=(\d{3})+(?!\d))/g, '.') + 'đ'
    );
  }

  // ✅ Lấy tham số từ URL
  getUrlParam(name: string): string | null {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
  }
}
