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
  orderSuccess = false;
  orderDetails: any = null;

  vietnamesePhonePattern = /^(0[35789])+([0-9]{8})$/;

  checkoutForm = new FormGroup({
    fullName: new FormControl('', [Validators.required, Validators.minLength(5)]),
    phone: new FormControl('', [Validators.required, Validators.pattern(this.vietnamesePhonePattern)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    address: new FormControl('', [Validators.required, Validators.minLength(10)]),
    paymentMethod: new FormControl('cod', [Validators.required]),
    notes: new FormControl(''),
  });

  public cartService = inject(CartService);
  private utils = inject(UtilsService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private accountService = inject(AccountService);
  private orderService = inject(OrderService); // Inject OrderService

  ngOnInit(): void {
    this.loadCartAndAccount();
  }

  private loadCartAndAccount(): void {
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
      this.markFormAsTouched();
      return;
    }

    const cartItems = this.cartService.getCartItems();
    if (cartItems.length === 0) {
      this.notify.error('Giỏ hàng của bạn đang trống!');
      return;
    }

    const orderData = this.buildOrderData(cartItems);
    this.submitOrder(orderData);
  }

  private markFormAsTouched(): void {
    Object.values(this.checkoutForm.controls).forEach(control => {
      control.markAsTouched();
    });
  }

  private buildOrderData(cartItems: ICartItem[]): any {
    const formValue = this.checkoutForm.value;
    return {
      customerInfo: {
        fullName: formValue.fullName || '',
        email: formValue.email || '',
        phone: formValue.phone || '',
        address: formValue.address || '',
        paymentMethod: formValue.paymentMethod || 'cod',
      },
      items: cartItems.map(item => ({
        productId: item.product.id,
        productName: item.product.name,
        quantity: item.quantity,
        price: item.product.price,
      })),
      totalAmount: this.cartService.getTotalPrice(),
      notes: formValue.notes || null,
    };
  }

  private submitOrder(orderData: any): void {
    this.orderService.create(orderData).subscribe({
      next: (response: any) => {
        this.cartService.clearCart();
        this.orderSuccess = true;
        this.orderDetails = {
          orderCode: response.body?.orderCode,
          orderId: response.body?.id,
          customerName: orderData.customerInfo.fullName,
          totalAmount: orderData.totalAmount,
          customerInfo: orderData.customerInfo,
        };
        this.notify.success('✅ Đặt hàng thành công!');
      },
      error: (error: any) => {
        console.error('Order creation failed');
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

  goToHome(): void {
    this.router.navigate(['/']);
  }

  continueShopping(): void {
    this.router.navigate(['/products']);
  }
}
