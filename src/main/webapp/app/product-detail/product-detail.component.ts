import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { IProduct } from 'app/entities/product/product.model';
import { CartService } from 'app/shared/services/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { LoginModalService } from 'app/core/login/login-modal.service';

@Component({
  selector: 'jhi-product-detail',
  standalone: true,
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
  imports: [CommonModule, RouterModule, FormsModule, FontAwesomeModule],
})
export class ProductDetailComponent implements OnInit {
  product: IProduct | null = null;
  account: Account | null = null;

  private route = inject(ActivatedRoute);
  private cartService = inject(CartService);
  private utils = inject(UtilsService);
  private notify = inject(NotificationService);
  public wishlistService = inject(WishlistService);
  private accountService = inject(AccountService);
  private loginModalService = inject(LoginModalService);
  private router = inject(Router);

  ngOnInit(): void {
    this.product = this.route.snapshot.data['product'];
    this.accountService
      .getAuthenticationState()
      .subscribe((account) => (this.account = account));
  }

  previousState(): void {
    window.history.back();
  }

  addToCart(product: IProduct): void {
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    this.cartService.addToCart(product.id!).subscribe(() => {
      this.notify.success('✅ Đã thêm sản phẩm vào giỏ hàng!');
      this.cartService.loadCart();
    });
  }

  toggleWishlist(product: IProduct): void {
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    this.wishlistService.toggleWishlist(product).subscribe({
      next: (added: boolean) => {
        if (added) {
          this.notify.success('💖 Đã thêm vào danh sách yêu thích!');
        } else {
          this.notify.info('💔 Đã xóa khỏi danh sách yêu thích!');
        }
      },
      error: (error: Error) => {
        this.notify.error(
          `❌ Lỗi khi cập nhật danh sách yêu thích: ${error.message}`,
        );
      },
    });
  }

  isInWishlist(productId: number): boolean {
    return this.wishlistService.isInWishlist(productId);
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
}
