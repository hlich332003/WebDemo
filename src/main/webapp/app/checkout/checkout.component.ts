import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject, of, forkJoin } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';

import { CartService, ICartItem } from 'app/shared/services/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { OrderService } from 'app/entities/order/order.service';

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
  isBuyNow = false; // Flag to track if this is a "Buy Now" order

  private readonly destroy$ = new Subject<void>();

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
  private route = inject(ActivatedRoute);
  private accountService = inject(AccountService);
  private orderService = inject(OrderService);

  ngOnInit(): void {
    this.loadCartAndAccount();
  }

  ngOnDestroy(): void {
    // Clear buy now item when leaving checkout if not successful
    if (!this.orderSuccess && this.isBuyNow) {
      this.cartService.clearBuyNowItem();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCartAndAccount(): void {
    // Check query params for buy-now mode
    const mode = this.route.snapshot.queryParamMap.get('mode');

    if (mode === 'buy-now') {
      // Buy Now mode - completely independent from cart
      const buyNowItem = this.cartService.getBuyNowItem();
      if (buyNowItem) {
        this.cart = [buyNowItem];
        this.isBuyNow = true;
      } else {
        // No buy now item found, redirect to home
        this.notify.warning('Không tìm thấy sản phẩm để mua.');
        this.router.navigate(['/']);
        return;
      }
    } else {
      // Regular checkout from cart
      this.cart = this.cartService.getItemsForCheckout();
      this.isBuyNow = false;

      // If no items selected from cart, redirect back to cart
      if (this.cart.length === 0) {
        this.notify.warning('Vui lòng chọn sản phẩm để thanh toán.');
        this.router.navigate(['/cart']);
        return;
      }
    }

    // Calculate total for items
    this.total = this.cart.reduce((sum, item) => sum + (item.product.price ?? 0) * item.quantity, 0);

    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
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

    if (this.cart.length === 0) {
      this.notify.error('Giỏ hàng của bạn đang trống!');
      return;
    }

    const orderData = this.buildOrderData(this.cart);
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
        product: item.product, // Send the whole product object
        quantity: item.quantity,
      })),
      totalAmount: this.total,
      notes: formValue.notes || null,
    };
  }

  private submitOrder(orderData: any): void {
    this.orderService
      .create(orderData)
      .pipe(
        switchMap((response: any) => {
          this.orderDetails = {
            orderCode: response.body?.orderCode,
            orderId: response.body?.id,
            customerName: orderData.customerInfo.fullName,
            totalAmount: orderData.totalAmount,
            customerInfo: orderData.customerInfo,
          };

          // If this is a Buy Now order, we DO NOT remove items from the cart
          if (this.isBuyNow) {
            this.cartService.clearBuyNowItem();
            return of(null);
          }

          // Otherwise, remove purchased items from cart
          const purchasedProductIds = this.cart.map(item => item.product.id);
          const removeObservables = purchasedProductIds.map(id => this.cartService.removeFromCart(id));

          if (removeObservables.length > 0) {
            return forkJoin(removeObservables).pipe(
              switchMap(() => {
                this.cartService.loadCart(); // Reload cart to reflect changes
                return of(null);
              }),
            );
          }
          return of(null);
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.orderSuccess = true;
          this.notify.success('✅ Đặt hàng thành công!');
        },
        error: (_error: any) => {
          console.error('Order creation failed', _error);
          this.notify.error('❌ Có lỗi xảy ra khi tạo đơn hàng. Vui lòng thử lại.');
        },
      });
  }

  previousState(): void {
    window.history.back();
  }

  goToHome(): void {
    this.router.navigate(['/'], { queryParams: { reload: Date.now() } });
  }

  continueShopping(): void {
    this.router.navigate(['/products'], {
      queryParams: { reload: Date.now() },
    });
  }
}
