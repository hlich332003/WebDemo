import { Component, inject } from '@angular/core';
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
export class CartComponent {
  public cartService = inject(CartService);
  private utils = inject(UtilsService);
  private notify = inject(NotificationService);
  public wishlistService = inject(WishlistService);

  updateQuantity(productId: number, quantity: number | string): void {
    const q = Number(quantity);
    if (!Number.isFinite(q) || q < 0) {
      this.notify.error('❌ Số lượng không hợp lệ!');
      return;
    }

    if (q === 0) {
      this.cartService.removeFromCart(productId);
      return;
    }

    const item = this.cartService
      .getCartItems()
      .find((i) => i.product.id === productId);
    if (!item) {
      return;
    }

    const availableStock = item.product.quantity ?? 0;
    if (q > availableStock) {
      this.notify.error('⚠️ Đã đạt giới hạn số lượng!');
      this.cartService.updateQuantity(productId, availableStock);
      return;
    }

    const success = this.cartService.updateQuantity(productId, q);
    if (!success) {
      this.notify.error('❌ Không thể cập nhật số lượng!');
    }
  }

  increaseQuantity(productId: number, currentQuantity: number): void {
    const item = this.cartService
      .getCartItems()
      .find((i) => i.product.id === productId);
    if (!item) {
      return;
    }

    const availableStock = item.product.quantity ?? 0;
    if (currentQuantity >= availableStock) {
      this.notify.error('⚠️ Đã đạt giới hạn số lượng!');
      return;
    }

    const success = this.cartService.updateQuantity(
      productId,
      currentQuantity + 1,
    );
    if (!success) {
      this.notify.error('⚠️ Không thể tăng số lượng!');
    }
  }

  decreaseQuantity(productId: number, currentQuantity: number): void {
    if (currentQuantity > 1) {
      this.cartService.updateQuantity(productId, currentQuantity - 1);
    } else {
      this.notify.info(
        '💡 Số lượng tối thiểu là 1. Dùng nút xóa nếu muốn bỏ sản phẩm.',
      );
    }
  }

  remove(productId: number): void {
    this.cartService.removeFromCart(productId);
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
        // Explicitly type error
        this.notify.error(
          `❌ Lỗi khi cập nhật danh sách yêu thích: ${error.message}`,
        );
      },
    });
  }

  isInWishlist(productId: number): boolean {
    return this.wishlistService.isInWishlist(productId);
  }
}
