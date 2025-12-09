import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import {
  ReactiveFormsModule,
  FormGroup,
  FormControl,
  Validators,
} from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { CartService, ICartItem } from 'app/shared/services/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { OrderService } from 'app/entities/order/order.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { IProduct } from 'app/entities/product/product.model';

@Component({
  selector: 'jhi-checkout',
  standalone: true,
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss'],
  imports: [CommonModule, RouterModule, ReactiveFormsModule, FontAwesomeModule],
})
export class CheckoutComponent implements OnInit, OnDestroy {
  cart: ICartItem[] = [];
  total = 0;
  account: Account | null = null;
  orderSuccess = false;
  orderDetails: any = null;

  private readonly destroy$ = new Subject<void>();

  vietnamesePhonePattern = /^(0[35789])+([0-9]{8})$/;

  checkoutForm = new FormGroup({
    fullName: new FormControl('', [
      Validators.required,
      Validators.minLength(5),
    ]),
    phone: new FormControl('', [
      Validators.required,
      Validators.pattern(this.vietnamesePhonePattern),
    ]),
    email: new FormControl('', [Validators.required, Validators.email]),
    address: new FormControl('', [
      Validators.required,
      Validators.minLength(10),
    ]),
    paymentMethod: new FormControl('cod', [Validators.required]),
    notes: new FormControl(''),
  });

  public cartService = inject(CartService);
  private utils = inject(UtilsService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private accountService = inject(AccountService);
  private orderService = inject(OrderService);
  public wishlistService = inject(WishlistService);

  ngOnInit(): void {
    this.loadCartAndAccount();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCartAndAccount(): void {
    this.cartService.cartItems$
      .pipe(takeUntil(this.destroy$))
      .subscribe((items) => {
        this.cart = items;
      });

    // Tối ưu: Lắng nghe totalPrice$
    this.cartService.totalPrice$
      .pipe(takeUntil(this.destroy$))
      .subscribe((total) => {
        this.total = total;
      });

    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe((account) => {
        this.account = account;
        if (this.account) {
          this.checkoutForm.patchValue({
            fullName:
              `${this.account.firstName ?? ''} ${this.account.lastName ?? ''}`.trim(),
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
    Object.values(this.checkoutForm.controls).forEach((control) => {
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
      items: cartItems.map((item) => ({
        productId: item.product.id,
        productName: item.product.name,
        quantity: item.quantity,
        price: item.product.price,
      })),
      // Tối ưu: Sử dụng giá trị đã được cập nhật
      totalAmount: this.total,
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
      error: (_error: any) => {
        console.error('Order creation failed');
        this.notify.error(
          '❌ Có lỗi xảy ra khi tạo đơn hàng. Vui lòng thử lại.',
        );
      },
    });
  }

  previousState(): void {
    window.history.back();
  }

  goToHome(): void {
    // Thêm query param để force reload dữ liệu sau khi checkout
    this.router.navigate(['/'], { queryParams: { reload: Date.now() } });
  }

  continueShopping(): void {
    // Thêm query param để force reload dữ liệu sau khi checkout
    this.router.navigate(['/products'], {
      queryParams: { reload: Date.now() },
    });
  }

  toggleWishlist(product: IProduct, event: Event): void {
    event.stopPropagation();
    const added = this.wishlistService.toggleWishlist(product);
    if (added) {
      this.notify.success('💖 Đã thêm vào danh sách yêu thích!');
    } else {
      this.notify.info('💔 Đã xóa khỏi danh sách yêu thích!');
    }
  }

  isInWishlist(productId: number): boolean {
    return this.wishlistService.isInWishlist(productId);
  }
}
