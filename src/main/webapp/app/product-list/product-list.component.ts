import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { combineLatest, forkJoin, Subject } from 'rxjs';
import {
  map,
  debounceTime,
  distinctUntilChanged,
  takeUntil,
} from 'rxjs/operators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { ProductService } from 'app/entities/product/product.service';
import { IProduct } from 'app/entities/product/product.model';
import { CategoryService } from 'app/entities/category/category.service';
import { ICategory } from 'app/entities/category/category.model';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { CartService } from 'app/shared/services/cart.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { ProductComparisonService } from 'app/shared/services/product-comparison.service';
import { LazyLoadImageDirective } from 'app/shared/directives/lazy-load-image.directive';
import { ITEMS_PER_PAGE } from 'app/config/pagination.constants';
import { SORT } from 'app/config/navigation.constants';
import { SortService, sortStateSignal } from 'app/shared/sort';
import { ItemCountComponent } from 'app/shared/pagination';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

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
  allProducts: IProduct[] = [];
  filteredProducts: IProduct[] = [];
  categories: ICategory[] = [];
  selectedCategorySlug = 'all';
  searchTerm = '';
  isLoading = false;
  isSearching = false; // Loading state riêng cho search

  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  sortState = sortStateSignal({});

  // Debounce search
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  // Cache - giảm xuống 30 giây
  private cache = new Map<string, { data: IProduct[]; timestamp: number }>();
  private readonly CACHE_DURATION = 30 * 1000; // 30 seconds

  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private utils = inject(UtilsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private notify = inject(NotificationService);
  private readonly cartService = inject(CartService);
  private readonly wishlistService = inject(WishlistService);
  private readonly comparisonService = inject(ProductComparisonService);
  private sortService = inject(SortService);

  ngOnInit(): void {
    // Clear cache mỗi khi component được khởi tạo
    this.clearCache();

    // Setup debounce cho search (chờ 500ms sau khi user gõ xong mới search)
    this.searchSubject
      .pipe(
        debounceTime(500), // Chờ 500ms
        distinctUntilChanged(), // Chỉ trigger khi giá trị thay đổi
        takeUntil(this.destroy$),
      )
      .subscribe((searchTerm) => {
        this.searchTerm = searchTerm;
        this.page = 1; // Reset về trang 1
        this.isSearching = true;
        this.loadAll();
      });

    this.handleNavigation();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Được gọi khi user gõ vào ô search
   */
  onSearchTermChange(event: Event): void {
    const term = (event.target as HTMLInputElement).value;
    this.searchSubject.next(term);
  }

  /**
   * Lấy tên danh mục từ slug
   */
  getCategoryName(): string {
    const category = this.categories.find(
      (c) => c.slug === this.selectedCategorySlug,
    );
    return category?.name || '';
  }

  /**
   * Clear search và reload tất cả sản phẩm
   */
  clearSearch(): void {
    this.searchTerm = '';
    this.searchSubject.next('');
  }

  /**
   * Clear tất cả filters
   */
  clearAllFilters(): void {
    this.searchTerm = '';
    this.selectedCategorySlug = 'all';
    this.page = 1;
    this.searchSubject.next('');
  }

  /**
   * Clear all cache - dùng khi cần reload dữ liệu mới
   */
  clearCache(): void {
    this.cache.clear();
  }

  loadAll(): void {
    const cacheKey = `${this.page}-${this.selectedCategorySlug}-${this.searchTerm}`;
    const cached = this.cache.get(cacheKey);

    // Check cache
    if (cached && Date.now() - cached.timestamp < this.CACHE_DURATION) {
      this.filteredProducts = cached.data;
      this.isSearching = false;
      return;
    }

    this.isLoading = true;
    forkJoin([
      this.categoryService.query().pipe(map((res) => res.body ?? [])),
      this.productService
        .query({
          page: this.page - 1,
          size: this.itemsPerPage,
          sort: this.sortService.buildSortParam(this.sortState()),
          // Sửa tên tham số lọc danh mục thành 'categorySlug'
          ...(this.selectedCategorySlug !== 'all' && {
            categorySlug: this.selectedCategorySlug,
          }),
          ...(this.searchTerm && { 'name.contains': this.searchTerm }),
        })
        .pipe(
          map((res) => {
            this.totalItems = Number(res.headers.get('X-Total-Count'));
            return res.body ?? [];
          }),
        ),
    ]).subscribe({
      next: ([categories, products]) => {
        this.categories = categories;
        this.allProducts = products;
        this.filteredProducts = this.allProducts;

        // Save to cache
        this.cache.set(cacheKey, {
          data: this.allProducts,
          timestamp: Date.now(),
        });

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

  addToCart(product: IProduct): void {
    // Kiểm tra số lượng tồn kho
    if (!product.quantity || product.quantity <= 0) {
      this.notify.error('❌ Sản phẩm đã hết hàng!');
      return;
    }

    // Kiểm tra số lượng đang có trong giỏ
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

    const productToAdd: IProduct = {
      ...product,
      price: product.price ?? 0,
    };

    const success = this.cartService.addToCart(productToAdd);

    if (success) {
      const newTotal = currentQtyInCart + 1;
      const remaining = product.quantity - newTotal;

      if (remaining <= 5 && remaining > 0) {
        this.notify.warning(
          `⚠️ Đã thêm vào giỏ hàng! Chỉ còn ${remaining} sản phẩm.`,
        );
      } else {
        this.notify.success('✅ Đã thêm sản phẩm vào giỏ hàng!');
      }
    } else {
      this.notify.error('⚠️ Không thể thêm sản phẩm vào giỏ hàng!');
    }
  }

  toggleWishlist(product: IProduct, event: Event): void {
    event.stopPropagation(); // Prevent navigation when clicking heart
    const added = this.wishlistService.toggleWishlist(product);
    if (added) {
      this.notify.success('💖 Đã thêm vào danh sách yêu thích!');
    } else {
      this.notify.info('💔 Đã xóa khỏi danh sách yêu thích!');
    }
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

  viewProductDetail(id: number): void {
    this.router.navigate(['/product', id]);
  }

  onCategoryChange(): void {
    this.page = 1;
    this.transition();
  }

  transition(): void {
    this.router.navigate(['./'], {
      relativeTo: this.route,
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        sort: this.sortService.buildSortParam(this.sortState()),
        categorySlug:
          this.selectedCategorySlug !== 'all'
            ? this.selectedCategorySlug
            : null,
        search: this.searchTerm || null,
      },
      queryParamsHandling: 'merge',
    });
  }

  private handleNavigation(): void {
    combineLatest([this.route.data, this.route.queryParamMap]).subscribe(
      ([data, params]) => {
        const page = params.get('page');
        this.page = +(page ?? 1);
        this.itemsPerPage = +(params.get('size') ?? ITEMS_PER_PAGE);
        this.sortState.set(
          this.sortService.parseSortParam(params.get(SORT) ?? data.defaultSort),
        );
        this.selectedCategorySlug = params.get('categorySlug') ?? 'all';
        this.searchTerm = params.get('search') ?? '';
        this.loadAll();
      },
    );
  }
}
