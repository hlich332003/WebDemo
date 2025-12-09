import { Injectable } from '@angular/core';
import { IProduct } from 'app/entities/product/product.model';

@Injectable({
  providedIn: 'root',
})
export class RecentlyViewedService {
  private readonly STORAGE_KEY = 'recently_viewed_products';
  private readonly MAX_ITEMS = 10;

  /**
   * Thêm sản phẩm vào danh sách đã xem
   */
  addProduct(product: IProduct): void {
    if (!product || !product.id) return;

    const recentlyViewed = this.getProducts();

    // Loại bỏ sản phẩm nếu đã tồn tại (để đưa lên đầu)
    const filtered = recentlyViewed.filter((p) => p.id !== product.id);

    // Thêm sản phẩm mới vào đầu danh sách
    filtered.unshift(product);

    // Giới hạn số lượng
    if (filtered.length > this.MAX_ITEMS) {
      filtered.pop();
    }

    // Lưu vào localStorage
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(filtered));
  }

  /**
   * Lấy danh sách sản phẩm đã xem
   */
  getProducts(): IProduct[] {
    try {
      const data = localStorage.getItem(this.STORAGE_KEY);
      return data ? JSON.parse(data) : [];
    } catch (error) {
      console.error('Error reading recently viewed products:', error);
      return [];
    }
  }

  /**
   * Xóa toàn bộ lịch sử
   */
  clearAll(): void {
    localStorage.removeItem(this.STORAGE_KEY);
  }

  /**
   * Xóa một sản phẩm khỏi danh sách
   */
  removeProduct(productId: number): void {
    const recentlyViewed = this.getProducts();
    const filtered = recentlyViewed.filter((p) => p.id !== productId);
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(filtered));
  }
}
