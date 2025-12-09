import { Injectable, signal, computed } from '@angular/core';
import { IProduct } from 'app/entities/product/product.model';

@Injectable({
  providedIn: 'root',
})
export class ProductComparisonService {
  private readonly STORAGE_KEY = 'webdemo_comparison';
  private readonly MAX_ITEMS = 4; // Tối đa 4 sản phẩm để so sánh

  // Signal để reactive updates
  private comparisonItems = signal<IProduct[]>([]);

  // Computed signals
  public items = computed(() => this.comparisonItems());
  public count = computed(() => this.comparisonItems().length);
  public isFull = computed(
    () => this.comparisonItems().length >= this.MAX_ITEMS,
  );

  constructor() {
    this.loadFromStorage();
  }

  /**
   * Load comparison list từ localStorage
   */
  private loadFromStorage(): void {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const items: IProduct[] = JSON.parse(stored);
        this.comparisonItems.set(items);
      }
    } catch (error) {
      console.error('Error loading comparison list from storage:', error);
      this.comparisonItems.set([]);
    }
  }

  /**
   * Save comparison list vào localStorage
   */
  private saveToStorage(): void {
    try {
      localStorage.setItem(
        this.STORAGE_KEY,
        JSON.stringify(this.comparisonItems()),
      );
    } catch (error) {
      console.error('Error saving comparison list to storage:', error);
    }
  }

  /**
   * Thêm sản phẩm vào danh sách so sánh
   */
  addToComparison(product: IProduct): boolean {
    if (!product.id) {
      return false;
    }

    // Kiểm tra đã đầy chưa
    if (this.comparisonItems().length >= this.MAX_ITEMS) {
      return false;
    }

    // Kiểm tra sản phẩm đã có chưa
    if (this.isInComparison(product.id)) {
      return false;
    }

    this.comparisonItems.update((items) => [...items, product]);
    this.saveToStorage();
    return true;
  }

  /**
   * Xóa sản phẩm khỏi danh sách so sánh
   */
  removeFromComparison(productId: number): void {
    this.comparisonItems.update((items) =>
      items.filter((item) => item.id !== productId),
    );
    this.saveToStorage();
  }

  /**
   * Toggle sản phẩm trong danh sách so sánh
   */
  toggleComparison(product: IProduct): boolean {
    if (!product.id) {
      return false;
    }

    if (this.isInComparison(product.id)) {
      this.removeFromComparison(product.id);
      return false;
    } else {
      return this.addToComparison(product);
    }
  }

  /**
   * Kiểm tra sản phẩm có trong danh sách so sánh không
   */
  isInComparison(productId: number): boolean {
    return this.comparisonItems().some((item) => item.id === productId);
  }

  /**
   * Xóa toàn bộ danh sách so sánh
   */
  clearAll(): void {
    this.comparisonItems.set([]);
    localStorage.removeItem(this.STORAGE_KEY);
  }

  /**
   * Lấy danh sách sản phẩm
   */
  getProducts(): IProduct[] {
    return this.comparisonItems();
  }
}
