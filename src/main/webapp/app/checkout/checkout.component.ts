import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';

import { CartService } from 'app/shared/cart/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';

@Component({
  selector: 'jhi-checkout',
  standalone: true,
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss'],
  imports: [CommonModule, RouterModule],
})
export class CheckoutComponent implements OnInit {
  cart: any[] = [];
  total = 0;

  constructor(
    public cartService: CartService,
    private utils: UtilsService,
    private notify: NotificationService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.cart = this.cartService.getSelectedItems(); // Chỉ lấy các sản phẩm đã được chọn
    this.total = this.cartService.getCartTotal();
  }

  formatPrice(price: string | number): string {
    return this.utils.formatPrice(price);
  }

  confirmPayment(event: Event): void {
    event.preventDefault();

    if (this.cart.length === 0) {
      this.notify.error('Giỏ hàng trống hoặc chưa có sản phẩm nào được chọn!');
      return;
    }

    const order = this.cartService.processPayment();
    if (order) {
      this.notify.success('✅ Thanh toán thành công! Cảm ơn bạn đã mua sắm.');
      setTimeout(() => {
        this.router.navigate(['/']);
      }, 2000);
    }
  }
}
