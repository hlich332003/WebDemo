import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { CartService } from 'app/shared/services/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { IProduct } from 'app/entities/product/product.model';

@Component({
  selector: 'jhi-cart',
  standalone: true,
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss'],
  imports: [CommonModule, RouterModule, FormsModule, FontAwesomeModule],
})
export class CartComponent implements OnInit {
  public cartService = inject(CartService);
  private utils = inject(UtilsService);
  private notify = inject(NotificationService);
  public wishlistService = inject(WishlistService);

  ngOnInit(): void {
    this.cartService.loadCart();
  }

  onQuantityBlur(productId: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const quantity = input.value.trim();

    if (quantity === '' || quantity === '0') {
      const item = this.cartService.getCartItems().find(i => i.product.id === productId);
      if (item) {
        input.value = item.quantity.toString();
      }
      return;
    }

    this.updateQuantity(productId, quantity);
  }

  updateQuantity(productId: number, quantity: number | string): void {
    const q = Number(quantity);
    if (!Number.isFinite(q) || q < 1) {
      this.notify.error('❌ Số lượng phải lớn hơn 0!');
      this.cartService.loadCart();
      return;
    }

    const item = this.cartService.getCartItems().find(i => i.product.id === productId);
    if (!item) {
      return;
    }

    const availableStock = item.product.quantity ?? 0;
    if (q > availableStock) {
      this.notify.error('⚠️ Đã đạt giới hạn số lượng!');
      this.cartService.updateQuantity(productId, availableStock).subscribe(() => this.cartService.loadCart());
      return;
    }

    this.cartService.updateQuantity(productId, q).subscribe({
      next: () => this.cartService.loadCart(),
      error: () => this.notify.error('❌ Không thể cập nhật số lượng!'),
    });
  }

  increaseQuantity(productId: number, currentQuantity: number): void {
    const item = this.cartService.getCartItems().find(i => i.product.id === productId);
    if (!item) {
      return;
    }

    const availableStock = item.product.quantity ?? 0;
    if (currentQuantity >= availableStock) {
      this.notify.error('⚠️ Đã đạt giới hạn số lượng!');
      return;
    }

    this.updateQuantity(productId, currentQuantity + 1);
  }

  decreaseQuantity(productId: number, currentQuantity: number): void {
    if (currentQuantity > 1) {
      this.updateQuantity(productId, currentQuantity - 1);
    } else {
      this.notify.info('💡 Số lượng tối thiểu là 1. Dùng nút xóa nếu muốn bỏ sản phẩm.');
    }
  }

  remove(productId: number): void {
    this.cartService.removeFromCart(productId).subscribe({
      next: () => {
        this.notify.success('Đã xóa sản phẩm khỏi giỏ hàng.');
        this.cartService.loadCart();
      },
      error: () => this.notify.error('❌ Không thể xóa sản phẩm!'),
    });
  }

  formatPrice(price: number | null | undefined): string {
    if (price === null || price === undefined) {
      return this.utils.formatPrice(0);
    }
    return this.utils.formatPrice(price);
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'content/images/default-product.svg';
  }

  toggleWishlist(product: IProduct, event: Event): void {
    event.stopPropagation();
    this.wishlistService.toggleWishlist(product).subscribe({
      next: (added: boolean) => {
        if (added) {
          this.notify.success('💖 Đã thêm vào danh sách yêu thích!');
        } else {
          this.notify.info('💔 Đã xóa khỏi danh sách yêu thích!');
        }
      },
      error: (error: Error) => {
        this.notify.error(`❌ Lỗi khi cập nhật danh sách yêu thích: ${error.message}`);
      },
    });
  }

  isInWishlist(productId: number): boolean {
    return this.wishlistService.isInWishlist(productId);
  }

  toggleSelection(productId: number): void {
    this.cartService.toggleItemSelected(productId);
  }

  toggleSelectAll(event: Event): void {
    const checkbox = event.target as HTMLInputElement;
    this.cartService.selectAllItems(checkbox.checked);
  }

  proceedToCheckout(): void {
    this.cartService.proceedToCheckout();
  }

  isAllSelected(): boolean {
    const items = this.cartService.getCartItems();
    return items.length > 0 && items.every(item => item.selected);
  }
}
