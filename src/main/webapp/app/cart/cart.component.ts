import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { CartService, ICartItem } from 'app/shared/services/cart.service'; // Sửa đường dẫn và import ICartItem
import { UtilsService } from 'app/shared/utils/utils.service';

@Component({
  selector: 'jhi-cart',
  standalone: true,
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss'],
  imports: [CommonModule, RouterModule, FormsModule],
})
export class CartComponent implements OnInit, OnDestroy {
  cart: ICartItem[] = [];
  total = 0;

  private readonly destroy$ = new Subject<void>();

  public cartService = inject(CartService);
  private utils = inject(UtilsService);

  ngOnInit(): void {
    this.cartService.cartItems$.pipe(takeUntil(this.destroy$)).subscribe(items => {
      this.cart = items;
      this.updateTotal();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  updateQuantity(productId: number, quantity: number | string): void {
    const q = Number(quantity);
    if (!Number.isFinite(q) || q < 0) {
      return;
    }
    if (q === 0) {
      this.cartService.removeFromCart(productId);
    } else {
      this.cartService.updateQuantity(productId, q);
    }
  }

  increaseQuantity(productId: number, currentQuantity: number): void {
    this.cartService.updateQuantity(productId, currentQuantity + 1);
  }

  decreaseQuantity(productId: number, currentQuantity: number): void {
    if (currentQuantity > 1) {
      this.cartService.updateQuantity(productId, currentQuantity - 1);
    }
  }

  remove(productId: number): void {
    this.cartService.removeFromCart(productId);
  }

  updateTotal(): void {
    this.total = this.cartService.getTotalPrice(); // Sử dụng getTotalPrice từ CartService
  }

  formatPrice(price: number | null | undefined): string {
    if (price === null || price === undefined) {
      return this.utils.formatPrice(0);
    }
    return this.utils.formatPrice(price);
  }
}
