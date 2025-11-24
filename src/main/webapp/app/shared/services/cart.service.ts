import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, map, shareReplay } from 'rxjs';
import { IProduct } from 'app/entities/product/product.model';

export interface ICartItem {
  product: IProduct;
  quantity: number;
}

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private cartItemsSubject: BehaviorSubject<ICartItem[]> = new BehaviorSubject<ICartItem[]>(this.getCartFromLocalStorage());
  public cartItems$: Observable<ICartItem[]> = this.cartItemsSubject.asObservable();

  // Tối ưu: Tạo các Observable cho dữ liệu được tính toán
  public totalQuantity$: Observable<number>;
  public totalPrice$: Observable<number>;

  constructor() {
    // Tính toán tổng số lượng
    this.totalQuantity$ = this.cartItems$.pipe(
      map(items => items.reduce((total, item) => total + item.quantity, 0)),
      shareReplay(1), // Cache lại kết quả cuối cùng
    );

    // Tính toán tổng giá tiền
    this.totalPrice$ = this.cartItems$.pipe(
      map(items => items.reduce((total, item) => total + (item.product.price ?? 0) * item.quantity, 0)),
      shareReplay(1), // Cache lại kết quả cuối cùng
    );
  }

  private getCartFromLocalStorage(): ICartItem[] {
    try {
      const cart = localStorage.getItem('cart');
      return cart ? JSON.parse(cart) : [];
    } catch (error) {
      console.error('Failed to load cart from localStorage:', error);
      return [];
    }
  }

  private saveCartToLocalStorage(cart: ICartItem[]): void {
    try {
      localStorage.setItem('cart', JSON.stringify(cart));
    } catch (error) {
      console.error('Failed to save cart to localStorage:', error);
    }
  }

  addToCart(product: IProduct, quantity: number = 1): boolean {
    if (!product?.id || quantity <= 0) {
      console.error('Invalid product or quantity');
      return false;
    }

    const availableStock = product.quantity ?? 0;
    if (availableStock <= 0) {
      console.warn('Product out of stock:', product.name);
      return false;
    }

    const currentCart = this.getCartItems(); // Lấy giá trị hiện tại
    const existingItem = currentCart.find(item => item.product.id === product.id);

    const currentQuantityInCart = existingItem ? existingItem.quantity : 0;
    const newTotalQuantity = currentQuantityInCart + quantity;

    if (newTotalQuantity > availableStock) {
      console.warn(
        `Cannot add ${quantity} items. Only ${availableStock - currentQuantityInCart} available (${currentQuantityInCart} already in cart)`,
      );
      return false;
    }

    if (existingItem) {
      existingItem.quantity += quantity;
    } else {
      currentCart.push({ product, quantity });
    }

    this.cartItemsSubject.next(currentCart);
    this.saveCartToLocalStorage(currentCart);
    return true;
  }

  updateQuantity(productId: number, quantity: number): boolean {
    const currentCart = this.getCartItems();
    const item = currentCart.find(i => i.product.id === productId);

    if (!item) {
      return false;
    }

    const availableStock = item.product.quantity ?? 0;

    if (quantity <= 0) {
      this.removeFromCart(productId);
      return true;
    }

    if (quantity > availableStock) {
      console.warn(`Cannot set quantity to ${quantity}. Only ${availableStock} available in stock`);
      return false;
    }

    item.quantity = quantity;
    this.cartItemsSubject.next(currentCart);
    this.saveCartToLocalStorage(currentCart);
    return true;
  }

  removeFromCart(productId: number): void {
    let currentCart = this.getCartItems();
    currentCart = currentCart.filter(item => item.product.id !== productId);
    this.cartItemsSubject.next(currentCart);
    this.saveCartToLocalStorage(currentCart);
  }

  clearCart(): void {
    try {
      this.cartItemsSubject.next([]);
      localStorage.removeItem('cart');
    } catch (error) {
      console.error('Failed to clear cart:', error);
    }
  }

  // Phương thức này vẫn hữu ích để lấy giá trị tức thời nếu cần
  getCartItems(): ICartItem[] {
    return this.cartItemsSubject.value;
  }
}
