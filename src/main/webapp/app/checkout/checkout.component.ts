import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { CartService, ICartItem } from 'app/shared/services/cart.service'; // Sửa đường dẫn và import ICartItem
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { OrderService } from 'app/entities/order/order.service'; // Import OrderService

@Component({
  selector: 'jhi-checkout',
  standalone: true,
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss'],
  imports: [CommonModule, RouterModule, ReactiveFormsModule, FontAwesomeModule],
})
export class CheckoutComponent implements OnInit {
  cart: ICartItem[] = [];
  total = 0;
  account: Account | null = null;

  vietnamesePhonePattern = /^(0[35789])+([0-9]{8})$/;

  checkoutForm = new FormGroup({
    fullName: new FormControl('', [Validators.required, Validators.minLength(5)]),
    phone: new FormControl('', [Validators.required, Validators.pattern(this.vietnamesePhonePattern)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    address: new FormControl('', [Validators.required, Validators.minLength(10)]),
    paymentMethod: new FormControl('cod', [Validators.required]),
  });

  public cartService = inject(CartService);
  private utils = inject(UtilsService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private accountService = inject(AccountService);
  private orderService = inject(OrderService); // Inject OrderService

  ngOnInit(): void {
    this.cartService.cartItems$.subscribe(items => {
      this.cart = items;
      this.updateTotal();
    });

    this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
      if (this.account) {
        this.checkoutForm.patchValue({
          fullName: `${this.account.firstName ?? ''} ${this.account.lastName ?? ''}`.trim(),
          email: this.account.email,
        });
      }
    });
  }

  formatPrice(price: number | null | undefined): string {
    if (price === null || price === undefined) {
      return this.utils.formatPrice(0);
    }
    return this.utils.formatPrice(price);
  }

  confirmPayment(): void {
    if (this.checkoutForm.invalid) {
      this.notify.error('Vui lòng kiểm tra lại thông tin khách hàng!');
      Object.values(this.checkoutForm.controls).forEach(control => {
        control.markAsTouched();
      });
      return;
    }

    const cartItems = this.cartService.getCartItems();
    if (cartItems.length === 0) {
      this.notify.error('Giỏ hàng của bạn đang trống!');
      return;
    }

    const orderData = {
      customerInfo: this.checkoutForm.value,
      items: cartItems.map(item => ({
        productId: item.product.id,
        productName: item.product.name,
        quantity: item.quantity,
        price: item.product.price,
      })),
      totalAmount: this.cartService.getTotalPrice(),
    };

    this.orderService.create(orderData).subscribe({
      next: () => {
        this.notify.success('✅ Thanh toán thành công! Đơn hàng của bạn đã được tạo.');
        this.cartService.clearCart();

        setTimeout(() => {
          this.router.navigate(['/account/my-orders']);
        }, 2000);
      },
      error: (error: any) => {
        // Đã thêm kiểu 'any' cho tham số error
        console.error('Lỗi khi tạo đơn hàng:', error);
        this.notify.error('❌ Có lỗi xảy ra khi tạo đơn hàng. Vui lòng thử lại.');
      },
    });
  }

  updateTotal(): void {
    this.total = this.cartService.getTotalPrice();
  }

  previousState(): void {
    window.history.back();
  }
}
