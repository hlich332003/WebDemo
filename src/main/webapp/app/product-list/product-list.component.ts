import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { combineLatest, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';

import { ProductService } from 'app/entities/product/product.service';
import { IProduct } from 'app/entities/product/product.model';
import { CategoryService } from 'app/entities/category/category.service';
import { ICategory } from 'app/entities/category/category.model';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { CartService } from 'app/shared/services/cart.service';
import { ITEMS_PER_PAGE } from 'app/config/pagination.constants';
import { SORT } from 'app/config/navigation.constants';
import { SortService, SortState, sortStateSignal } from 'app/shared/sort';
import { ItemCountComponent } from 'app/shared/pagination';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'jhi-product-list',
  standalone: true,
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss'],
  imports: [CommonModule, FormsModule, RouterModule, ItemCountComponent, NgbPaginationModule],
})
export class ProductListComponent implements OnInit {
  allProducts: IProduct[] = [];
  filteredProducts: IProduct[] = [];
  categories: ICategory[] = [];
  selectedCategorySlug = 'all';
  searchTerm = '';
  isLoading = false;
  featuredProducts: IProduct[] = [];

  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  sortState = sortStateSignal({});

  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private utils = inject(UtilsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private notify = inject(NotificationService);
  private readonly cartService = inject(CartService);
  private sortService = inject(SortService);

  ngOnInit(): void {
    this.handleNavigation();
  }

  loadAll(): void {
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
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        console.error('Error loading products');
      },
    });
  }

  filterAndSearch(): void {
    this.transition();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.selectedCategorySlug = 'all';
    this.transition();
  }

  addToCart(product: IProduct): void {
    const productToAdd: IProduct = {
      ...product,
      price: product.price ?? 0,
      quantity: product.quantity ?? 0,
    };
    this.cartService.addToCart(productToAdd);
    this.notify.success('Đã thêm sản phẩm vào giỏ hàng!');
  }

  formatPrice(price: number | null | undefined): string {
    if (price === null || price === undefined) {
      return this.utils.formatPrice(0);
    }
    return this.utils.formatPrice(price);
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

  protected onSearchTermChange($event: any) {}
}
