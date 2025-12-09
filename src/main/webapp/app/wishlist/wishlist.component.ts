import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';

import {
  WishlistService,
  WishlistItem,
} from 'app/shared/services/wishlist.service';
import { CartService } from 'app/shared/services/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';

@Component({
  selector: 'jhi-wishlist',
  standalone: true,
  templateUrl: './wishlist.component.html',
  styleUrls: ['./wishlist.component.scss'],
  imports: [CommonModule, RouterModule],
})
export class WishlistComponent {
  private wishlistService = inject(WishlistService);
  private cartService = inject(CartService);
  private utils = inject(UtilsService);
  private notify = inject(NotificationService);
  private router = inject(Router);

  // Reactive wishlist items
  wishlistItems = this.wishlistService.items;
  wishlistCount = this.wishlistService.count;

  removeFromWishlist(productId: number): void {
    this.wishlistService.removeFromWishlist(productId);
    this.notify.success('✅ Đã xóa khỏi danh sách yêu thích!');
  }

  addToCart(item: WishlistItem): void {
    const product = item.product;

    // Kiểm tra tồn kho
    if (!product.quantity || product.quantity <= 0) {
      this.notify.error('❌ Sản phẩm đã hết hàng!');
      return;
    }

    const success = this.cartService.addToCart(product);
    if (success) {
      this.notify.success('✅ Đã thêm vào giỏ hàng!');
      // Tùy chọn: xóa khỏi wishlist sau khi thêm vào cart
      // this.wishlistService.removeFromWishlist(product.id!);
    } else {
      this.notify.error('❌ Không thể thêm vào giỏ hàng!');
    }
  }

  viewProductDetail(productId: number): void {
    this.router.navigate(['/product', productId]);
  }

  clearWishlist(): void {
    if (confirm('Bạn có chắc muốn xóa toàn bộ danh sách yêu thích?')) {
      this.wishlistService.clearWishlist();
      this.notify.success('✅ Đã xóa toàn bộ danh sách yêu thích!');
    }
  }

  formatPrice(price: number | null | undefined): string {
    return this.utils.formatPrice(price ?? 0);
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'content/images/default-product.svg';
  }
}
