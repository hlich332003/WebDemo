import { Injectable, signal, computed } from '@angular/core';
import { IProduct } from 'app/entities/product/product.model';

export interface WishlistItem {
  product: IProduct;
  addedAt: Date;
}

@Injectable({
  providedIn: 'root',
})
export class WishlistService {
  private readonly STORAGE_KEY = 'webdemo_wishlist';

  // Signal để reactive updates
  private wishlistItems = signal<WishlistItem[]>([]);

  // Computed signals
  public items = computed(() => this.wishlistItems());
  public count = computed(() => this.wishlistItems().length);

  constructor() {
    this.loadFromStorage();
  }

  /**
   * Load wishlist từ localStorage
   */
  private loadFromStorage(): void {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const items: WishlistItem[] = JSON.parse(stored);
        // Convert string dates back to Date objects
        items.forEach((item) => {
          item.addedAt = new Date(item.addedAt);
        });
        this.wishlistItems.set(items);
      }
    } catch (error) {
      console.error('Error loading wishlist from storage:', error);
      this.wishlistItems.set([]);
    }
  }

  /**
   * Save wishlist vào localStorage
   */
  private saveToStorage(): void {
    try {
      localStorage.setItem(
        this.STORAGE_KEY,
        JSON.stringify(this.wishlistItems()),
      );
    } catch (error) {
      console.error('Error saving wishlist to storage:', error);
    }
  }

  /**
   * Thêm sản phẩm vào wishlist
   */
  addToWishlist(product: IProduct): boolean {
    if (!product.id) {
      return false;
    }

    // Kiểm tra xem sản phẩm đã có trong wishlist chưa
    if (this.isInWishlist(product.id)) {
      return false;
    }

    const newItem: WishlistItem = {
      product,
      addedAt: new Date(),
    };

    this.wishlistItems.update((items) => [...items, newItem]);
    this.saveToStorage();
    return true;
  }

  /**
   * Xóa sản phẩm khỏi wishlist
   */
  removeFromWishlist(productId: number): void {
    this.wishlistItems.update((items) =>
      items.filter((item) => item.product.id !== productId),
    );
    this.saveToStorage();
  }

  /**
   * Toggle sản phẩm trong wishlist
   */
  toggleWishlist(product: IProduct): boolean {
    if (!product.id) {
      return false;
    }

    if (this.isInWishlist(product.id)) {
      this.removeFromWishlist(product.id);
      return false;
    } else {
      return this.addToWishlist(product);
    }
  }

  /**
   * Kiểm tra sản phẩm có trong wishlist không
   */
  isInWishlist(productId: number): boolean {
    return this.wishlistItems().some((item) => item.product.id === productId);
  }

  /**
   * Lấy tất cả items trong wishlist
   */
  getWishlistItems(): WishlistItem[] {
    return this.wishlistItems();
  }

  /**
   * Xóa toàn bộ wishlist
   */
  clearWishlist(): void {
    this.wishlistItems.set([]);
    this.saveToStorage();
  }

  /**
   * Lấy số lượng items trong wishlist
   */
  getWishlistCount(): number {
    return this.wishlistItems().length;
  }
}
