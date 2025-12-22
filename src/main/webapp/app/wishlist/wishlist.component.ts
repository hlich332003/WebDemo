import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable } from 'rxjs';

import { WishlistService } from 'app/shared/services/wishlist.service';
import { CartService } from 'app/shared/services/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { IProduct } from 'app/entities/product/product.model';
import { AccountService } from 'app/core/auth/account.service';
import { LoginModalService } from 'app/core/login/login-modal.service';
import { LazyLoadImageDirective } from 'app/shared/directives/lazy-load-image.directive';

@Component({
  selector: 'jhi-wishlist',
  standalone: true,
  templateUrl: './wishlist.component.html',
  styleUrls: ['./wishlist.component.scss'],
  imports: [
    CommonModule,
    RouterModule,
    FontAwesomeModule,
    LazyLoadImageDirective,
  ],
})
export class WishlistComponent implements OnInit {
  public wishlistService = inject(WishlistService);
  private cartService = inject(CartService);
  private utils = inject(UtilsService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private accountService = inject(AccountService);
  private loginModalService = inject(LoginModalService);

  wishlistItems$: Observable<IProduct[]> = this.wishlistService.items$;

  ngOnInit(): void {
    this.wishlistService.loadWishlist();
  }

  removeFromWishlist(product: IProduct): void {
    this.wishlistService.toggleWishlist(product).subscribe({
      next: () => {
        this.notify.info('💔 Đã xóa khỏi danh sách yêu thích!');
        this.wishlistService.loadWishlist();
      },
      error: (error: Error) => {
        this.notify.error(`❌ Lỗi khi xóa: ${error.message}`);
        this.wishlistService.loadWishlist();
      },
    });
  }

  addToCart(product: IProduct): void {
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    if (!product.quantity || product.quantity <= 0) {
      this.notify.error('❌ Sản phẩm đã hết hàng!');
      return;
    }
    this.cartService.addToCart(product.id).subscribe(() => {
      this.notify.success('✅ Đã thêm vào giỏ hàng!');
      this.cartService.loadCart();
    });
  }

  buyNow(product: IProduct): void {
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    if (product.id && product.quantity && product.quantity > 0) {
      this.cartService.addToCart(product.id).subscribe(() => {
        this.cartService.loadCart();
        this.router.navigate(['/checkout']);
      });
    }
  }

  isInCart(productId: number): boolean {
    return this.cartService
      .getCartItems()
      .some((item) => item.product.id === productId);
  }

  viewProductDetail(productId: number): void {
    this.router.navigate(['/product', productId]);
  }

  formatPrice(price: number | null | undefined): string {
    return this.utils.formatPrice(price ?? 0);
  }

  getProxiedImageUrl(imageUrl: string | null | undefined): string {
    if (!imageUrl) {
      return 'content/images/no-product-image.png';
    }
    return `/api/public/image-proxy?url=${encodeURIComponent(imageUrl)}`;
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'content/images/default-product.svg';
  }
}
