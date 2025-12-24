import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, map } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { IProduct } from 'app/entities/product/product.model';
import { ProductService } from 'app/entities/product/product.service';
import { ICategory } from 'app/entities/category/category.model';
import { RecentlyViewedService } from 'app/shared/services/recently-viewed.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { CartService } from 'app/shared/services/cart.service';
import { LoginModalService } from 'app/core/login/login-modal.service';
import { LazyLoadImageDirective } from 'app/shared/directives/lazy-load-image.directive';
import { NotificationService } from 'app/shared/notification/notification.service';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule, LazyLoadImageDirective],
})
export class HomeComponent implements OnInit, OnDestroy {
  account: Account | null = null;
  bestSellerProducts: IProduct[] = [];
  newProducts: IProduct[] = [];
  recentlyViewedProducts: IProduct[] = [];
  featuredCategories: ICategory[] = [];
  isLoading = false;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private accountService: AccountService,
    private productService: ProductService,
    private recentlyViewedService: RecentlyViewedService,
    private wishlistService: WishlistService,
    private cartService: CartService,
    private loginModalService: LoginModalService,
    private router: Router,
    private notificationService: NotificationService,
  ) {}

  ngOnInit(): void {
    this.loadAll(); // Load data immediately

    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        this.account = account;

        if (account?.authorities.includes('ROLE_ADMIN')) {
          this.router.navigate(['/admin']);
        }
      });
  }

  loadAll(): void {
    this.isLoading = true;
    this.productService.query({ sort: ['salesCount,desc'], size: 5 }).subscribe(res => {
      this.bestSellerProducts = res.body ?? [];
      this.isLoading = false;
    });
    this.productService.query({ sort: ['createdDate,desc'], size: 5 }).subscribe(res => {
      this.newProducts = res.body ?? [];
    });
    this.recentlyViewedProducts = this.recentlyViewedService.getProducts();
    this.loadFeaturedCategories();
  }

  loadFeaturedCategories(): void {
    this.productService
      .getCategories()
      .pipe(map((res: HttpResponse<ICategory[]>) => res.body ?? []))
      .subscribe((categories: ICategory[]) => {
        const allProductsCategory: ICategory = {
          id: 0,
          name: 'Tất cả sản phẩm',
          slug: '',
        };
        const unclassified = categories.find(c => c.slug === 'chua-phan-loai');
        const otherCategories = categories
          .filter(c => c.slug !== 'chua-phan-loai')
          .sort((a, b) => (a.name ?? '').localeCompare(b.name ?? ''));

        const sortedCategories = [allProductsCategory, ...otherCategories];
        if (unclassified) {
          sortedCategories.push(unclassified);
        }

        this.featuredCategories = sortedCategories;
      });
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  viewProductDetail(productId: number): void {
    this.router.navigate(['/product', productId]);
  }

  addToCart(product: IProduct): void {
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    if (product.id) {
      this.cartService.addToCart(product.id).subscribe({
        next: () => {
          this.cartService.loadCart();
          this.notificationService.success('Đã thêm sản phẩm vào giỏ hàng');
        },
        error: err => {
          console.error('Error adding to cart:', err);
          this.notificationService.error('Không thể thêm vào giỏ hàng. Vui lòng thử lại.');
        },
      });
    }
  }

  buyNow(product: IProduct): void {
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    if (product.id && product.quantity && product.quantity > 0) {
      this.cartService.addToCart(product.id).subscribe({
        next: () => {
          this.cartService.loadCart();
          this.router.navigate(['/checkout']);
        },
        error: err => {
          console.error('Error adding to cart:', err);
          this.notificationService.error('Không thể thêm vào giỏ hàng. Vui lòng thử lại.');
        },
      });
    }
  }

  toggleWishlist(product: IProduct, event: MouseEvent): void {
    event.stopPropagation();
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    this.wishlistService.toggleWishlist(product).subscribe();
  }

  isInWishlist(productId: number): boolean {
    return this.wishlistService.isInWishlist(productId);
  }

  formatPrice(price: number | null | undefined): string {
    if (price === null || price === undefined) {
      return 'N/A';
    }
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  }

  getProxiedImageUrl(imageUrl: string | null | undefined): string {
    if (!imageUrl) {
      return 'content/images/no-product-image.png';
    }
    return `/api/public/image-proxy?url=${encodeURIComponent(imageUrl)}`;
  }

  onImageError(event: Event): void {
    (event.target as HTMLImageElement).src = 'content/images/no-product-image.png';
  }

  getCategoryIcon(slug: string | null | undefined): string {
    if (!slug) return 'box';
    switch (slug) {
      case 'dien-thoai':
        return 'mobile-alt';
      case 'may-tinh-bang':
        return 'tablet-alt';
      case 'laptop':
        return 'laptop';
      case 'phu-kien':
        return 'headphones';
      default:
        return 'box';
    }
  }
}
