import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { combineLatest, Subject } from 'rxjs';
import {
  map,
  debounceTime,
  distinctUntilChanged,
  takeUntil,
  tap,
} from 'rxjs/operators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { ProductService } from 'app/entities/product/product.service';
import { IProduct } from 'app/entities/product/product.model';
import { ICategory } from 'app/entities/product/category.model';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { CartService } from 'app/shared/services/cart.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { LazyLoadImageDirective } from 'app/shared/directives/lazy-load-image.directive';
import { ITEMS_PER_PAGE } from 'app/config/pagination.constants';
import { ItemCountComponent } from 'app/shared/pagination';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { AccountService } from 'app/core/auth/account.service';
import { LoginModalService } from 'app/core/login/login-modal.service';

@Component({
  selector: 'jhi-product-list',
  standalone: true,
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    ItemCountComponent,
    NgbPaginationModule,
    FontAwesomeModule,
    LazyLoadImageDirective,
  ],
})
export class ProductListComponent implements OnInit, OnDestroy {
  filteredProducts: IProduct[] = [];
  categories: ICategory[] = [];

  // Filter state
  selectedCategorySlug = 'all';
  searchTerm = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  inStockOnly = false;
  sortOption = 'createdDate,desc';

  isLoading = false;
  isSearching = false;

  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;

  // Track wishlist items for realtime updates
  private wishlistProductIds = new Set<number>();

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  private productService = inject(ProductService);
  private utils = inject(UtilsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private notify = inject(NotificationService);
  private readonly cartService = inject(CartService);
  public readonly wishlistService = inject(WishlistService);
  private accountService = inject(AccountService);
  private loginModalService = inject(LoginModalService);

  ngOnInit(): void {
    this.loadCategories();
    this.searchSubject
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.applyFilters();
      });

    // Subscribe to wishlist changes for realtime updates
    this.wishlistService.items$
      .pipe(takeUntil(this.destroy$))
      .subscribe((items) => {
        this.wishlistProductIds = new Set(
          items
            .map((item) => item.id)
            .filter((id): id is number => id !== undefined),
        );
      });

    this.handleNavigation();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchTermChange(event: Event): void {
    const term = (event.target as HTMLInputElement).value;
    this.searchSubject.next(term);
  }

  applyFilters(): void {
    this.page = 1;
    this.transition();
  }

  clearAllFilters(): void {
    this.searchTerm = '';
    this.selectedCategorySlug = 'all';
    this.minPrice = null;
    this.maxPrice = null;
    this.inStockOnly = false;
    this.sortOption = 'createdDate,desc';
    this.applyFilters();
  }

  loadAll(): void {
    this.isLoading = true;
    this.productService
      .query({
        page: this.page - 1,
        size: this.itemsPerPage,
        sort: this.sortOption.split(','),
        ...(this.selectedCategorySlug !== 'all' && {
          categorySlug: this.selectedCategorySlug,
        }),
        ...(this.searchTerm && { nameContains: this.searchTerm }),
        ...(this.minPrice != null && { minPrice: this.minPrice }),
        ...(this.maxPrice != null && { maxPrice: this.maxPrice }),
        ...(this.inStockOnly && { inStock: true }),
      })
      .pipe(
        tap((res) => {
          this.totalItems = Number(res.headers.get('X-Total-Count'));
          this.filteredProducts = res.body ?? [];
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.isSearching = false;
        },
        error: (error) => {
          this.isLoading = false;
          this.isSearching = false;
          console.error('Error loading products:', error);
          this.notify.error('❌ Không thể tải danh sách sản phẩm!');
        },
      });
  }

  loadCategories(): void {
    this.productService
      .getCategories()
      .pipe(
        map((res) => res.body ?? []),
        map((cats) => {
          const unclassified = cats.find((c) => c.slug === 'chua-phan-loai');
          const others = cats.filter((c) => c.slug !== 'chua-phan-loai');
          return unclassified ? [...others, unclassified] : others;
        }),
      )
      .subscribe((sortedCats) => (this.categories = sortedCats));
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
    return this.wishlistProductIds.has(productId);
  }

  isInCart(productId: number): boolean {
    return this.cartService
      .getCartItems()
      .some((item) => item.product.id === productId);
  }

  formatPrice(price: number | null | undefined): string {
    return this.utils.formatPrice(price ?? 0);
  }

  getProxiedImageUrl(imageUrl: string | null | undefined): string {
    return imageUrl || 'content/images/default-product.svg';
  }

  onImageError(event: Event): void {
    (event.target as HTMLImageElement).src =
      'content/images/default-product.svg';
  }

  viewProductDetail(id: number): void {
    this.router.navigate(['/product', id]);
  }

  transition(): void {
    this.router.navigate(['./'], {
      relativeTo: this.route,
      queryParams: {
        page: this.page,
        sort: this.sortOption,
        category:
          this.selectedCategorySlug !== 'all'
            ? this.selectedCategorySlug
            : null,
        search: this.searchTerm || null,
        minPrice: this.minPrice,
        maxPrice: this.maxPrice,
        inStock: this.inStockOnly ? true : null,
      },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private handleNavigation(): void {
    combineLatest([this.route.queryParamMap])
      .pipe(takeUntil(this.destroy$))
      .subscribe(([params]) => {
        this.page = +(params.get('page') ?? 1);
        this.sortOption = params.get('sort') ?? 'createdDate,desc';
        this.selectedCategorySlug = params.get('category') ?? 'all';
        this.searchTerm = params.get('search') ?? '';
        this.minPrice = params.has('minPrice')
          ? +params.get('minPrice')!
          : null;
        this.maxPrice = params.has('maxPrice')
          ? +params.get('maxPrice')!
          : null;
        this.inStockOnly = params.get('inStock') === 'true';
        this.loadAll();
      });
  }
}
