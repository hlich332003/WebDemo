import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';

import { CartService } from 'app/shared/cart/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';

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
    private cartService: CartService,
    private utils: UtilsService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.cart = this.cartService.getCart();
    this.total = this.cartService.getCartTotal();
  }

  formatPrice(price: string | number): string {
    return this.utils.formatPrice(price);
  }

  confirmPayment(event: Event): void {
    event.preventDefault();

    if (this.cart.length === 0) {
      this.utils.showNotification('Giỏ hàng trống!', 'error');
      return;
    }

    const order = this.cartService.processPayment();
    if (order) {
      this.utils.showNotification('✅ Thanh toán thành công! Cảm ơn bạn đã mua sắm.', 'success');
      setTimeout(() => {
        this.router.navigate(['/home']);
      }, 2000);
    }
  }
}
