import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  HttpHeaders,
  HttpResponse,
  HttpErrorResponse,
} from '@angular/common/http';
import { combineLatest } from 'rxjs';
import { NgbModal, NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { HttpClient } from '@angular/common/http';

import SharedModule from 'app/shared/shared.module';
import {
  SortByDirective,
  SortDirective,
  SortService,
  SortState,
  sortStateSignal,
} from 'app/shared/sort';
import { ITEMS_PER_PAGE } from 'app/config/pagination.constants';
import { SORT } from 'app/config/navigation.constants';
import { ProductService } from 'app/entities/product/product.service';
import { IProduct } from 'app/entities/product/product.model';
import ProductDeleteDialogComponent from '../delete/product-delete-dialog.component';
import { NotificationService } from 'app/shared/notification/notification.service';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { ItemCountComponent } from 'app/shared/pagination';

@Component({
  selector: 'jhi-product-management',
  standalone: true,
  templateUrl: './product-management.component.html',
  imports: [
    RouterModule,
    SharedModule,
    SortDirective,
    SortByDirective,
    NgbPaginationModule,
    ItemCountComponent,
  ],
})
export default class ProductManagementComponent implements OnInit {
  products = signal<IProduct[] | null>(null);
  isLoading = signal(false);
  totalItems = signal(0);
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  sortState = sortStateSignal({});
  searchTerm = signal<string>('');
  private searchTimeout?: number;

  private readonly productService = inject(ProductService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sortService = inject(SortService);
  private readonly modalService = inject(NgbModal);
  private readonly notify = inject(NotificationService);
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  ngOnInit(): void {
    this.handleNavigation();
  }

  trackIdentity(index: number, item: IProduct): number {
    return item.id ?? index;
  }

  deleteProduct(product: IProduct): void {
    const modalRef = this.modalService.open(ProductDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.product = product;
    modalRef.closed.subscribe((reason) => {
      if (reason === 'deleted') {
        this.loadAll();
      }
    });
  }

  loadAll(): void {
    if (!this.page) this.page = 1;
    this.isLoading.set(true);
    const params: any = {
      page: this.page - 1,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState(), 'id'),
    };

    if (this.searchTerm()) {
      params['name.contains'] = this.searchTerm();
    }

    this.productService.query(params).subscribe({
      next: (res: HttpResponse<IProduct[]>) => {
        this.isLoading.set(false);
        this.onSuccess(res.body, res.headers);
      },
      error: (error) => {
        console.error('Failed to load products:', error);
        this.isLoading.set(false);
        this.notify.error('Không thể tải danh sách sản phẩm.');
      },
    });
  }

  onSearchChange(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.searchTimeout = window.setTimeout(() => {
      this.page = 1;
      this.loadAll();
    }, 300);
  }

  clearSearch(): void {
    this.searchTerm.set('');
    this.page = 1;
    this.loadAll();
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchTerm.set(value);
    this.onSearchChange();
  }

  onClearSearch(): void {
    this.clearSearch();
  }

  transition(sortState?: SortState): void {
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: {
        page: this.page,
        sort: this.sortService.buildSortParam(sortState ?? this.sortState()),
      },
      queryParamsHandling: 'merge',
    });
  }

  pageChange(page: number): void {
    this.page = page;
    this.transition();
  }

  toggleFeatured(product: IProduct): void {
    if (!product.id) {
      this.notify.error('ID sản phẩm không hợp lệ.');
      return;
    }
    this.productService.toggleFeatured(product.id).subscribe({
      next: () => {
        this.loadAll();
        this.notify.success('Sản phẩm đã được cập nhật.');
      },
      error: (error) => {
        console.error('Failed to toggle featured status:', error);
        this.notify.error('Cập nhật trạng thái nổi bật thất bại!');
      },
    });
  }

  exportProducts(): void {
    this.http
      .get(
        this.applicationConfigService.getEndpointFor(
          'api/admin/export/products',
        ),
        { responseType: 'blob' },
      )
      .subscribe({
        next: (data: Blob) => {
          this.downloadFile(data, 'products.xlsx');
          this.notify.success('Export sản phẩm thành công!');
        },
        error: (error) => {
          console.error('Failed to export products:', error);
          this.notify.error('Export sản phẩm thất bại!');
        },
      });
  }

  importProducts(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];
    const formData = new FormData();
    formData.append('file', file, file.name);

    this.http
      .post(
        this.applicationConfigService.getEndpointFor(
          'api/admin/import/products',
        ),
        formData,
      )
      .subscribe({
        next: () => {
          this.notify.success('Import sản phẩm thành công!');
          this.loadAll();
          input.value = '';
        },
        error: (error: HttpErrorResponse) => {
          console.error('Failed to import products');
          const errorMessage =
            error.error?.message ??
            error.error?.detail ??
            'Import sản phẩm thất bại.';
          this.notify.error(errorMessage);
          input.value = '';
        },
      });
  }

  private downloadFile(data: Blob, filename: string): void {
    const blob = new Blob([data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  private handleNavigation(): void {
    combineLatest([
      this.activatedRoute.data,
      this.activatedRoute.queryParamMap,
    ]).subscribe(([data, params]) => {
      const page = params.get('page');
      this.page = +(page ?? 1);
      this.sortState.set(
        this.sortService.parseSortParam(params.get(SORT) ?? data.defaultSort),
      );
      this.loadAll();
    });
  }

  private onSuccess(products: IProduct[] | null, headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')));
    if (products) {
      this.products.set(products);
    }
  }
}
