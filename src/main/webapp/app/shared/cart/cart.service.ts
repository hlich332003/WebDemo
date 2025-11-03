import { Injectable } from '@angular/core';
import { ProductService } from '../product/product.service';
import { NotificationService } from '../notification/notification.service';
import { CONFIG } from 'app/shared/config/config.constants';

@Injectable({ providedIn: 'root' })
export class CartService {
  private cart: any[] = [];
  private readonly CART_KEY = CONFIG.CART_STORAGE_KEY;
  private readonly ORDER_KEY = CONFIG.ORDERS_STORAGE_KEY;

  constructor(
    private productService: ProductService,
    private notify: NotificationService,
  ) {
    this.loadCart();
  }

  loadCart(): void {
    const stored = localStorage.getItem(this.CART_KEY);
    try {
      this.cart = stored ? JSON.parse(stored) : [];
      this.cart.forEach(item => {
        if (item.selected === undefined) {
          item.selected = true;
        }
      });
    } catch (e) {
      console.error('❌ Lỗi parse giỏ hàng:', e);
      this.cart = [];
    }
  }

  saveCart(): void {
    localStorage.setItem(this.CART_KEY, JSON.stringify(this.cart));
  }

  getCart(): any[] {
    return [...this.cart];
  }

  getSelectedItems(): any[] {
    return this.cart.filter(item => item.selected);
  }

  getNumericPrice(price: string | number): number {
    if (typeof price === 'string') {
      return parseInt(price.replace(/\./g, ''));
    }
    return price;
  }

  getCartTotal(): number {
    return this.cart.reduce((total, item) => {
      if (item.selected) {
        const price = this.getNumericPrice(item.price);
        return total + price * item.quantity;
      }
      return total;
    }, 0);
  }

  getCartItemsCount(): number {
    return this.cart.reduce((count, item) => count + item.quantity, 0);
  }

  addToCart(productId: number, quantity = 1): boolean {
    const product = this.productService.findById(productId);
    if (!product) {
      this.notify.error('Sản phẩm không tồn tại!');
      return false;
    }

    if (product.stock < quantity) {
      this.notify.error(`"${product.name}" chỉ còn ${product.stock} sản phẩm!`);
      return false;
    }

    const item = this.cart.find(i => i.id === productId);
    if (item) {
      if (product.stock < item.quantity + quantity) {
        this.notify.error(`Số lượng vượt quá tồn kho!`);
        return false;
      }
      item.quantity += quantity;
      item.selected = true;
    } else {
      this.cart.push({
        id: product.id,
        name: product.name,
        price: product.price,
        image: product.image,
        quantity,
        selected: true,
      });
    }

    this.saveCart();
    this.notify.success(`✅ Đã thêm "${product.name}" vào giỏ hàng!`);
    return true;
  }

  removeFromCart(productId: number): void {
    this.cart = this.cart.filter(item => item.id !== productId);
    this.saveCart();
    this.notify.info('Đã xóa sản phẩm khỏi giỏ hàng');
  }

  updateQuantity(productId: number, quantity: number): void {
    if (quantity < 1) {
      this.removeFromCart(productId);
      return;
    }

    const product = this.productService.findById(productId);
    if (!product) {
      this.notify.error('Sản phẩm không tồn tại!');
      return;
    }

    if (product.stock < quantity) {
      this.notify.error(`Chỉ còn ${product.stock} sản phẩm trong kho!`);
      return;
    }

    const item = this.cart.find(i => i.id === productId);
    if (item) {
      item.quantity = quantity;
      this.saveCart();
    }
  }

  toggleItemSelected(productId: number): void {
    const item = this.cart.find(i => i.id === productId);
    if (item) {
      item.selected = !item.selected;
      this.saveCart();
    }
  }

  toggleAllItems(selected: boolean): void {
    this.cart.forEach(item => (item.selected = selected));
    this.saveCart();
  }

  processPayment(): any | false {
    const selectedItems = this.cart.filter(item => item.selected);

    if (selectedItems.length === 0) {
      this.notify.error('Vui lòng chọn ít nhất một sản phẩm để thanh toán!');
      return false;
    }

    for (const item of selectedItems) {
      const product = this.productService.findById(item.id);
      if (!product || product.stock < item.quantity) {
        this.notify.error(`"${item.name}" không đủ số lượng!`);
        return false;
      }
    }

    for (const item of selectedItems) {
      const product = this.productService.findById(item.id);
      if (product) {
        product.stock -= item.quantity;
        this.productService.updateStock(item.id, product.stock);
      }
    }

    const order = {
      id: Date.now(),
      items: [...selectedItems],
      total: this.getCartTotal(),
      date: new Date().toISOString(),
    };

    this.saveOrder(order);

    this.cart = this.cart.filter(item => !item.selected);
    this.saveCart();

    this.notify.success('✅ Thanh toán thành công!');
    return order;
  }

  saveOrder(order: any): void {
    const orders = JSON.parse(localStorage.getItem(this.ORDER_KEY) || '[]');
    orders.push(order);
    localStorage.setItem(this.ORDER_KEY, JSON.stringify(orders));
  }
}
