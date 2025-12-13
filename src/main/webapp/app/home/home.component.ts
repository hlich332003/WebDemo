import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  computed,
  inject,
} from '@angular/core';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { Subject, forkJoin, of } from 'rxjs';
import { takeUntil, filter, map, switchMap } from 'rxjs/operators';
import SharedModule from 'app/shared/shared.module';
import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { ProductService } from 'app/entities/product/product.service';
import { IProduct } from 'app/entities/product/product.model';
import { ICategory } from 'app/entities/product/category.model';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { CartService } from 'app/shared/services/cart.service';
import { RecentlyViewedService } from 'app/shared/services/recently-viewed.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { ProductComparisonService } from 'app/shared/services/product-comparison.service';
import { LoginModalService } from 'app/core/login/login-modal.service';
import { IconName } from '@fortawesome/fontawesome-svg-core';
import { HttpResponse } from '@angular/common/http';

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
  newProducts: IProduct[] = [];
  bestSellerProducts: IProduct[] = [];
  recentlyViewedProducts: IProduct[] = [];
  isLoading = false;
  bannerImageUrl =
    'https://thuthuatnhanh.com/wp-content/uploads/2021/06/Hinh-anh-dan-PC-khung-dep-me-hon.jpg';

  // Track wishlist items for realtime updates
  wishlistItems = signal<IProduct[]>([]);

  private readonly destroy$ = new Subject<void>();

  private readonly accountService = inject(AccountService);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
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
    console.warn('🎯 HomeComponent ngOnInit called');

    // Load data immediately on init
    this.loadAllData();

    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe((account) => {
        this.account.set(account);
      });

    // Subscribe to wishlist changes for realtime updates
    this.wishlistService.items$
      .pipe(takeUntil(this.destroy$))
      .subscribe((items) => {
        this.wishlistItems.set(items);
      });

    this.router.events
      .pipe(
        filter(
          (event): event is NavigationEnd => event instanceof NavigationEnd,
        ),
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
    console.warn('🚀 loadAllData() called');
    this.isLoading = true;
    const productService = this.productService;

    // Use main endpoints as they are already public in SecurityConfiguration
    const query = productService.query.bind(productService);
    const getCategories = productService.getCategories.bind(productService);

    console.warn('📡 Starting to load data...');

    const newProducts$ = query({
      page: 0,
      size: 10,
      sort: ['id,desc'],
    }).pipe(map((res: HttpResponse<IProduct[]>) => res.body ?? []));

    const bestSellerProducts$ = query({
      page: 0,
      size: 10,
      sort: ['salesCount,desc'],
    }).pipe(map((res: HttpResponse<IProduct[]>) => res.body ?? []));

    const categoriesWithProducts$ = getCategories().pipe(
      map((res: HttpResponse<ICategory[]>) => res.body ?? []),
      switchMap((categories) => {
        if (!categories || categories.length === 0) {
          return of([]);
        }
        const categoryProductRequests = categories.map((category) =>
          query({
            categorySlug: category.slug,
            page: 0,
            size: 5,
            sort: ['id,desc'],
          }).pipe(
            map((res: HttpResponse<IProduct[]>) => {
              (category as any).products = res.body ?? [];
              return category;
            }),
          ),
        );
        return forkJoin(categoryProductRequests);
      }),
      map((categoriesWithProducts) =>
        categoriesWithProducts.filter((c) => (c as any).products.length > 0),
      ),
    );

    forkJoin({
      newProducts: newProducts$,
      bestSellerProducts: bestSellerProducts$,
      featuredCategories: categoriesWithProducts$,
    }).subscribe({
      next: ({ newProducts, bestSellerProducts, featuredCategories }) => {
        console.warn('✅ Data loaded:', {
          newProducts: newProducts.length,
          bestSellerProducts: bestSellerProducts.length,
          featuredCategories: featuredCategories.length,
        });

        this.newProducts = newProducts;
        this.bestSellerProducts = bestSellerProducts;

        const unclassified = featuredCategories.find(
          (c: ICategory) => c.slug === 'chua-phan-loai',
        );
        const others = featuredCategories.filter(
          (c: ICategory) => c.slug !== 'chua-phan-loai',
        );
        this.featuredCategories = unclassified
          ? [...others, unclassified]
          : others;

        console.warn('✅ Featured categories:', this.featuredCategories);

        this.recentlyViewedProducts = this.recentlyViewedService
          .getProducts()
          .slice(0, 10);
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        console.error('❌ Error loading home page data', err);
        this.notify.error('❌ Đã có lỗi xảy ra khi tải dữ liệu trang chủ.');
      },
    });
  }

  getCategoryIcon(slug: string | null | undefined): IconName {
    switch (slug) {
      case 'man-hinh':
        return 'desktop';
      case 'pc-gaming':
        return 'gamepad';
      case 'linh-kien':
        return 'microchip';
      case 'electronics':
        return 'plug';
      case 'books':
        return 'book';
      default:
        return 'tags';
    }
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
    this.cartService.addToCart(product.id!).subscribe(() => {
      this.notify.success('✅ Đã thêm sản phẩm vào giỏ hàng!');
      this.cartService.loadCart();
    });
  }

  toggleWishlist(product: IProduct, event: Event): void {
    event.stopPropagation();
    if (!this.accountService.isAuthenticated()) {
      this.loginModalService.open();
      return;
    }
    const isInWishlist = this.wishlistService.isInWishlist(product.id!);
    this.wishlistService.toggleWishlist(product).subscribe({
      next: () => {
        if (isInWishlist) {
          this.notify.info('💔 Đã xóa khỏi danh sách yêu thích!');
        } else {
          this.notify.success('💖 Đã thêm vào danh sách yêu thích!');
        }
      },
    });
  }

  isInWishlist(productId: number): boolean {
    return this.wishlistItems().some((item) => item.id === productId);
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
    return this.utils.formatPrice(price ?? 0);
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
