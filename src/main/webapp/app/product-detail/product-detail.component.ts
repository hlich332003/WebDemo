import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { IProduct } from 'app/entities/product/product.model';
import { CartService } from 'app/shared/services/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { LoginModalService } from 'app/core/login/login-modal.service';
import { RecentlyViewedService } from 'app/shared/services/recently-viewed.service';

@Component({
  selector: 'jhi-product-detail',
  standalone: true,
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
  imports: [CommonModule, RouterModule, FormsModule, FontAwesomeModule],
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  product: IProduct | null = null;
  account: Account | null = null;

  // Track wishlist items for realtime updates
  private wishlistProductIds = new Set<number>();
  private destroy$ = new Subject<void>();

  private route = inject(ActivatedRoute);
  private cartService = inject(CartService);
  private utils = inject(UtilsService);
  private notify = inject(NotificationService);
  public wishlistService = inject(WishlistService);
  private accountService = inject(AccountService);
  private loginModalService = inject(LoginModalService);
  private router = inject(Router);
  private recentlyViewedService = inject(RecentlyViewedService);

  ngOnInit(): void {
    this.product = this.route.snapshot.data['product'];
    if (this.product) {
      this.recentlyViewedService.addProduct(this.product);
    }
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => (this.account = account));

    // Subscribe to wishlist changes for realtime updates
    this.wishlistService.items$.pipe(takeUntil(this.destroy$)).subscribe(items => {
      this.wishlistProductIds = new Set(items.map(item => item.id).filter((id): id is number => id !== undefined));
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  previousState(): void {
    window.history.back();
  }

  addToCart(product: IProduct): void {
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    this.cartService.addToCart(product.id).subscribe(() => {
      this.notify.success('✅ Đã thêm sản phẩm vào giỏ hàng!');
      this.cartService.loadCart();
    });
  }

  buyNow(product: IProduct): void {
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    if (product.id && product.quantity && product.quantity > 0) {
      // Navigate directly to checkout with buy-now mode, completely independent from cart
      this.cartService.setBuyNowItem(product, 1);
      this.router.navigate(['/checkout'], {
        queryParams: { mode: 'buy-now' },
      });
    } else {
      this.notify.error('Sản phẩm này hiện không còn hàng!');
    }
  }

  toggleWishlist(product: IProduct): void {
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    const isInWishlist = this.wishlistService.isInWishlist(product.id);
    this.wishlistService.toggleWishlist(product).subscribe({
      next: () => {
        if (isInWishlist) {
          this.notify.info('💔 Đã xóa khỏi danh sách yêu thích!');
        } else {
          this.notify.success('💖 Đã thêm vào danh sách yêu thích!');
        }
      },
      error: (error: Error) => {
        this.notify.error(`❌ Lỗi khi cập nhật danh sách yêu thích: ${error.message}`);
      },
    });
  }

  isInWishlist(productId: number): boolean {
    return this.wishlistProductIds.has(productId);
  }

  formatPrice(price: number | null | undefined): string {
    return this.utils.formatPrice(price ?? 0);
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'content/images/default-product.svg';
  }
}
