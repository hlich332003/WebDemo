import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import {
  takeUntil,
  debounceTime,
  distinctUntilChanged,
  map,
} from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { InfiniteScrollDirective } from 'ngx-infinite-scroll';

import { IProduct } from 'app/entities/product/product.model';
import { ProductService } from 'app/entities/product/product.service';
import { ICategory } from 'app/entities/category/category.model';
import { CartService } from 'app/shared/services/cart.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { AccountService } from 'app/core/auth/account.service';
import { LoginModalService } from 'app/core/login/login-modal.service';
import SharedModule from 'app/shared/shared.module';
import { LazyLoadImageDirective } from 'app/shared/directives/lazy-load-image.directive';

@Component({
  selector: 'jhi-product-list',
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    FontAwesomeModule,
    NgbModule,
    SharedModule,
    LazyLoadImageDirective,
    InfiniteScrollDirective,
  ],
})
export class ProductListComponent implements OnInit, OnDestroy {
  products: IProduct[] = [];
  categories: ICategory[] = [];

  isLoading = false;

  page = 0;
  itemsPerPage = 20;
  totalItems = 0;
  hasMore = true;

  searchTerm = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  selectedCategorySlug = 'all';
  inStockOnly = false;
  sortOption = 'name,asc';

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private wishlistService: WishlistService,
    private accountService: AccountService,
    private loginModalService: LoginModalService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe((params) => {
        // Reset list when filters change
        this.products = [];
        this.page = 0;
        this.hasMore = true;

        this.sortOption = params['sort'] || 'name,asc';
        this.searchTerm = params['search'] || '';
        this.selectedCategorySlug = params['categorySlug'] || 'all';
        this.minPrice = params['minPrice'] ? +params['minPrice'] : null;
        this.maxPrice = params['maxPrice'] ? +params['maxPrice'] : null;
        this.inStockOnly = params['inStock'] === 'true';
        this.loadProducts();
      });

    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.applyFiltersAndNavigate();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadProducts(): void {
    if (this.isLoading || !this.hasMore) {
      return;
    }

    this.isLoading = true;
    const queryParams: any = {
      page: this.page,
      size: this.itemsPerPage,
      sort: this.sortOption.split(','),
    };

    if (this.searchTerm) {
      queryParams['name'] = this.searchTerm;
    }
    if (this.selectedCategorySlug && this.selectedCategorySlug !== 'all') {
      queryParams['categorySlug'] = this.selectedCategorySlug;
    }
    if (this.minPrice !== null) {
      queryParams['minPrice'] = this.minPrice;
    }
    if (this.maxPrice !== null) {
      queryParams['maxPrice'] = this.maxPrice;
    }
    if (this.inStockOnly) {
      queryParams['inStock'] = true;
    }

    this.productService.query(queryParams).subscribe({
      next: (res) => {
        const newProducts = res.body ?? [];
        this.products = [...this.products, ...newProducts];
        this.totalItems = Number(res.headers.get('X-Total-Count'));
        this.hasMore = this.products.length < this.totalItems;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  onScroll(): void {
    if (this.hasMore && !this.isLoading) {
      this.page++;
      this.loadProducts();
    }
  }

  loadCategories(): void {
    this.productService
      .getCategories()
      .pipe(map((res: HttpResponse<ICategory[]>) => res.body ?? []))
      .subscribe((cats: ICategory[]) => {
        const unclassified = cats.find((c) => c.slug === 'chua-phan-loai');
        const others = cats
          .filter((c) => c.slug !== 'chua-phan-loai')
          .sort((a, b) => (a.name ?? '').localeCompare(b.name ?? ''));
        this.categories = unclassified ? [...others, unclassified] : others;
      });
  }

  applyFiltersAndNavigate(): void {
    // Reset page is handled in route subscription
    const queryParams: any = {
      sort: this.sortOption,
      search: this.searchTerm || null,
      categorySlug: this.selectedCategorySlug,
      minPrice: this.minPrice,
      maxPrice: this.maxPrice,
      inStock: this.inStockOnly ? true : null,
    };

    Object.keys(queryParams).forEach(
      (key) =>
        (queryParams[key] == null || queryParams[key] === '') &&
        delete queryParams[key],
    );

    this.router.navigate(['/products'], {
      queryParams,
      queryParamsHandling: 'merge',
    });
  }

  onSearchTermChange(event: Event): void {
    this.searchTerm = (event.target as HTMLInputElement).value;
    this.searchSubject.next(this.searchTerm);
  }

  clearAllFilters(): void {
    this.searchTerm = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.selectedCategorySlug = 'all';
    this.inStockOnly = false;
    this.applyFiltersAndNavigate();
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
      this.cartService.addToCart(product.id).subscribe(() => {
        this.cartService.loadCart();
      });
    }
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
    (event.target as HTMLImageElement).src =
      'content/images/no-product-image.png';
  }
}
