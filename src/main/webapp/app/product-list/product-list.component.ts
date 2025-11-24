import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { combineLatest, forkJoin, Subject } from 'rxjs';
import { map, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { ProductService } from 'app/entities/product/product.service';
import { IProduct } from 'app/entities/product/product.model';
import { CategoryService } from 'app/entities/category/category.service';
import { ICategory } from 'app/entities/category/category.model';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { CartService } from 'app/shared/services/cart.service';
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
  imports: [CommonModule, FormsModule, RouterModule, ItemCountComponent, NgbPaginationModule],
})
export class ProductListComponent implements OnInit, OnDestroy {
  allProducts: IProduct[] = [];
  filteredProducts: IProduct[] = [];
  categories: ICategory[] = [];
  selectedCategorySlug = 'all';
  searchTerm = '';
  isLoading = false;
  isSearching = false; // Loading state riêng cho search
  featuredProducts: IProduct[] = [];

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
      .subscribe(searchTerm => {
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
  onSearchTermChange(term: string): void {
    this.searchSubject.next(term);
  }

  /**
   * Clear search và reload tất cả sản phẩm
   */
  clearSearch(): void {
    this.searchTerm = '';
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
      return;
    }

    this.isLoading = true;
    forkJoin([
      this.categoryService.query().pipe(map(res => res.body ?? [])),
      this.productService
        .query({
          page: this.page - 1,
          size: this.itemsPerPage,
          sort: this.sortService.buildSortParam(this.sortState()),
          // Sửa tên tham số lọc danh mục thành 'categorySlug'
          ...(this.selectedCategorySlug !== 'all' && { categorySlug: this.selectedCategorySlug }),
          ...(this.searchTerm && { 'name.contains': this.searchTerm }),
        })
        .pipe(
          map(res => {
            this.totalItems = Number(res.headers.get('X-Total-Count'));
            return res.body ?? [];
          }),
        ),
      this.productService.getFeaturedProducts().pipe(map(res => res.body ?? [])),
    ]).subscribe({
      next: ([categories, products, featuredProds]) => {
        this.categories = categories;
        this.allProducts = products;
        this.featuredProducts = featuredProds;
        this.filteredProducts = this.allProducts;

        // Save to cache
        const cacheKey = `${this.page}-${this.selectedCategorySlug}-${this.searchTerm}`;
        this.cache.set(cacheKey, { data: this.allProducts, timestamp: Date.now() });

        this.isLoading = false;
        this.isSearching = false; // Tắt loading indicator của search
      },
      error: () => {
        this.isLoading = false;
        this.isSearching = false;
        console.error('Error loading products');
      },
    });
  }

  addToCart(product: IProduct): void {
    // Kiểm tra số lượng tồn kho
    if (!product.quantity || product.quantity <= 0) {
      this.notify.error('❌ Sản phẩm đã hết hàng!');
      return;
    }

    const productToAdd: IProduct = {
      ...product,
      price: product.price ?? 0,
    };

    const success = this.cartService.addToCart(productToAdd);

    if (success) {
      this.notify.success('✅ Đã thêm sản phẩm vào giỏ hàng!');
    } else {
      this.notify.error('⚠️ Không thể thêm sản phẩm vào giỏ hàng!');
    }
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
        categorySlug: this.selectedCategorySlug !== 'all' ? this.selectedCategorySlug : null,
        search: this.searchTerm || null,
      },
      queryParamsHandling: 'merge',
    });
  }

  private handleNavigation(): void {
    combineLatest([this.route.data, this.route.queryParamMap]).subscribe(([data, params]) => {
      const page = params.get('page');
      this.page = +(page ?? 1);
      this.itemsPerPage = +(params.get('size') ?? ITEMS_PER_PAGE);
      this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data.defaultSort));
      this.selectedCategorySlug = params.get('categorySlug') ?? 'all';
      this.searchTerm = params.get('search') ?? '';
      this.loadAll();
    });
  }
}
