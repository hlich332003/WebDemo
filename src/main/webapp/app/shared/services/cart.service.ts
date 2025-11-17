import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
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

  constructor() {}

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

  addToCart(product: IProduct, quantity: number = 1): void {
    if (!product?.id || quantity <= 0) {
      console.error('Invalid product or quantity');
      return;
    }

    const currentCart = this.cartItemsSubject.value;
    const existingItem = currentCart.find(item => item.product.id === product.id);

    if (existingItem) {
      existingItem.quantity += quantity;
    } else {
      currentCart.push({ product, quantity });
    }

    this.cartItemsSubject.next(currentCart);
    this.saveCartToLocalStorage(currentCart);
  }

  updateQuantity(productId: number, quantity: number): void {
    const currentCart = this.cartItemsSubject.value;
    const item = currentCart.find(i => i.product.id === productId);

    if (item) {
      if (quantity > 0) {
        item.quantity = quantity;
      } else {
        this.removeFromCart(productId);
        return;
      }
    }

    this.cartItemsSubject.next(currentCart);
    this.saveCartToLocalStorage(currentCart);
  }

  removeFromCart(productId: number): void {
    let currentCart = this.cartItemsSubject.value;
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

  getCartItems(): ICartItem[] {
    return this.cartItemsSubject.value;
  }

  getTotalQuantity(): number {
    return this.cartItemsSubject.value.reduce((total, item) => total + item.quantity, 0);
  }

  getTotalPrice(): number {
    return this.cartItemsSubject.value.reduce((total, item) => total + (item.product.price ?? 0) * item.quantity, 0);
  }
}
