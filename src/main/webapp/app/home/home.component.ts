import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  computed,
  inject,
} from '@angular/core';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil, filter, map } from 'rxjs/operators';
import SharedModule from 'app/shared/shared.module';
import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { ProductService } from 'app/entities/product/product.service';
import { IProduct } from 'app/entities/product/product.model';
import { CategoryService } from 'app/entities/category/category.service';
import { ICategory } from 'app/entities/category/category.model';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { CartService } from 'app/shared/services/cart.service';
import { RecentlyViewedService } from 'app/shared/services/recently-viewed.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { ProductComparisonService } from 'app/shared/services/product-comparison.service';
import { LoginModalService } from 'app/core/login/login-modal.service';

@Component({
  selector: 'jhi-home',
  standalone: true,
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  imports: [SharedModule, RouterModule],
})
export class HomeComponent implements OnInit, OnDestroy {
  account = signal<Account | null>(null);
  featuredCategories: ICategory[] = [];
  products: IProduct[] = [];
  newProducts: IProduct[] = [];
  bestSellerProducts: IProduct[] = [];
  recentlyViewedProducts: IProduct[] = [];
  isLoading = false;
  bannerImageUrl =
    'https://thuthuatnhanh.com/wp-content/uploads/2021/06/Hinh-anh-dan-PC-khung-dep-me-hon.jpg';

  private readonly destroy$ = new Subject<void>();

  private readonly accountService = inject(AccountService);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly utils = inject(UtilsService);
  private readonly notify = inject(NotificationService);
  private readonly cartService = inject(CartService);
  private readonly recentlyViewedService = inject(RecentlyViewedService);
  public readonly wishlistService = inject(WishlistService);
  private readonly comparisonService = inject(ProductComparisonService);
  private readonly loginModalService = inject(LoginModalService);

  isAdmin = computed(() => {
    const currentAccount = this.account();
    return currentAccount && currentAccount.authorities.includes('ROLE_ADMIN');
  });

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe((account) => this.account.set(account));

    this.loadAllData();

    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        filter(
          (event: NavigationEnd) => event.url === '/' || event.url === '/home',
        ),
        takeUntil(this.destroy$),
      )
      .subscribe(() => {
        this.loadAllData();
      });
  }

  loadAllData(): void {
    this.isLoading = true;
    forkJoin([
      this.categoryService.query().pipe(map((res) => res.body ?? [])),
      this.productService.query().pipe(map((res) => res.body ?? [])),
    ]).subscribe({
      next: ([featuredCats, allProducts]) => {
        this.featuredCategories = featuredCats;
        this.products = allProducts;
        this.newProducts = allProducts.slice(0, 12);
        const pinned = allProducts.filter((p) => p.isPinned === true);
        const notPinned = allProducts.filter((p) => p.isPinned !== true);
        notPinned.sort((a, b) => (b.salesCount ?? 0) - (a.salesCount ?? 0));
        const lowStockCandidates = notPinned.filter(
          (p) =>
            p.quantity !== null && p.quantity !== undefined && p.quantity < 50,
        );
        const combined: IProduct[] = [
          ...pinned,
          ...lowStockCandidates,
          ...notPinned,
        ];
        this.bestSellerProducts = combined.slice(0, 8);
        this.featuredCategories.forEach((category) => {
          category.products = this.products.filter(
            (product) => product.category?.id === category.id,
          );
        });
        this.recentlyViewedProducts = this.recentlyViewedService
          .getProducts()
          .slice(0, 6);
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        console.error('Error loading home page data');
      },
    });
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  viewProductDetail(id: number): void {
    this.router.navigate(['/product', id]);
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
    const currentCartItem = this.cartService
      .getCartItems()
      .find((item) => item.product.id === product.id);
    const currentQtyInCart = currentCartItem ? currentCartItem.quantity : 0;
    if (currentQtyInCart >= product.quantity) {
      this.notify.error(
        `⚠️ Bạn đã có ${currentQtyInCart} sản phẩm này trong giỏ! Tồn kho chỉ còn ${product.quantity}.`,
      );
      return;
    }
    this.cartService.addToCart(product.id!).subscribe(() => {
      const newTotal = currentQtyInCart + 1;
      const remaining = product.quantity! - newTotal;
      if (remaining <= 5 && remaining > 0) {
        this.notify.warning(
          `⚠️ Đã thêm vào giỏ hàng! Chỉ còn ${remaining} sản phẩm.`,
        );
      } else {
        this.notify.success('✅ Đã thêm sản phẩm vào giỏ hàng!');
      }
      this.cartService.loadCart();
    });
  }

  toggleWishlist(product: IProduct, event: Event): void {
    event.stopPropagation();
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

  toggleComparison(product: IProduct, event: Event): void {
    event.stopPropagation();
    const added = this.comparisonService.toggleComparison(product);
    if (added) {
      this.notify.success('📊 Đã thêm vào danh sách so sánh!');
    } else {
      if (this.comparisonService.isFull()) {
        this.notify.warning('⚠️ Chỉ có thể so sánh tối đa 4 sản phẩm!');
      } else {
        this.notify.info('❌ Đã xóa khỏi danh sách so sánh!');
      }
    }
  }

  isInComparison(productId: number): boolean {
    return this.comparisonService.isInComparison(productId);
  }

  formatPrice(price: number | null | undefined): string {
    if (price === null || price === undefined) {
      return this.utils.formatPrice(0);
    }
    return this.utils.formatPrice(price);
  }

  getProxiedImageUrl(imageUrl: string | null | undefined): string {
    return imageUrl || 'content/images/default-product.svg';
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'content/images/default-product.svg';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
