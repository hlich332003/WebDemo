import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { CartService } from 'app/shared/cart/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';

@Component({
  selector: 'jhi-cart',
  standalone: true,
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss'],
  imports: [CommonModule, RouterModule, FormsModule],
})
export class CartComponent implements OnInit {
  cart: any[] = [];
  isAllSelected = true;
  total = 0;

  constructor(
    public cartService: CartService,
    private utils: UtilsService,
  ) {}

  ngOnInit(): void {
    this.loadCartData();
  }

  loadCartData(): void {
    this.cart = this.cartService.getCart();
    this.updateTotal();
    this.updateSelectAllState();
  }

  updateQuantity(id: number, quantity: number): void {
    this.cartService.updateQuantity(id, quantity);
    this.loadCartData(); // Tải lại toàn bộ là cần thiết khi số lượng thay đổi
  }

  remove(id: number): void {
    this.cartService.removeFromCart(id);
    this.loadCartData(); // Tải lại toàn bộ là cần thiết khi xóa sản phẩm
  }

  updateTotal(): void {
    this.total = this.cartService.getCartTotal();
  }

  formatPrice(price: string | number): string {
    return this.utils.formatPrice(price);
  }

  // Tối ưu hóa: Chỉ cập nhật các giá trị cần thiết, không tải lại toàn bộ giỏ hàng
  onItemToggle(): void {
    this.updateTotal();
    this.updateSelectAllState();
    this.cartService.saveCart(); // Lưu lại trạng thái mới của giỏ hàng
  }

  onSelectAllToggle(): void {
    this.cart.forEach(item => (item.selected = this.isAllSelected));
    this.updateTotal();
    this.cartService.saveCart(); // Lưu lại trạng thái mới của giỏ hàng
  }

  updateSelectAllState(): void {
    if (this.cart.length === 0) {
      this.isAllSelected = false;
    } else {
      this.isAllSelected = this.cart.every(item => item.selected);
    }
  }
}
