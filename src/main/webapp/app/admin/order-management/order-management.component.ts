import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { combineLatest } from 'rxjs';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';

import { IOrder } from './order.model';
import { OrderManagementService } from './order-management.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { ItemCountComponent } from 'app/shared/pagination';
import { SortByDirective, SortDirective, SortService, SortState, sortStateSignal } from 'app/shared/sort';
import { ITEMS_PER_PAGE } from 'app/config/pagination.constants';
import { SORT } from 'app/config/navigation.constants';

@Component({
  selector: 'jhi-order-management',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FontAwesomeModule,
    FormsModule,
    SortDirective,
    SortByDirective,
    ItemCountComponent,
    NgbPaginationModule,
  ],
  templateUrl: './order-management.component.html',
  styleUrls: ['./order-management.component.scss'],
})
export class OrderManagementComponent implements OnInit {
  orders = signal<IOrder[] | null>(null);
  isLoading = signal(false);
  totalItems = signal(0);
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  sortState = sortStateSignal({});
  selectedOrder = signal<IOrder | null>(null);
  searchOrderCode = signal<string>('');
  private searchTimeout: any;

  private orderService = inject(OrderManagementService);
  private notify = inject(NotificationService);
  private activatedRoute = inject(ActivatedRoute);
  private router = inject(Router);
  private sortService = inject(SortService);

  ngOnInit(): void {
    this.handleNavigation();
  }

  loadAll(): void {
    this.isLoading.set(true);
    const queryParams: any = {
      page: this.page - 1,
      size: this.itemsPerPage,
      sort: ['id,desc'],
    };

    // Thêm tìm kiếm theo mã đơn hàng nếu có
    if (this.searchOrderCode()) {
      queryParams.orderCode = this.searchOrderCode();
    }

    this.orderService.query(queryParams).subscribe({
      next: (res: HttpResponse<IOrder[]>) => {
        this.isLoading.set(false);
        this.onSuccess(res.body, res.headers);
      },
      error: error => {
        console.error('Failed to load orders:', error);
        this.isLoading.set(false);
        this.orders.set([]);
        this.notify.error('Không thể tải danh sách đơn hàng.');
      },
    });
  }

  trackId(index: number, item: IOrder): number {
    return item.id ?? 0;
  }

  formatOrderDate(date: Date | null | undefined): string {
    if (!date) {
      return 'N/A';
    }
    const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' };
    return new Date(date).toLocaleDateString('vi-VN', options);
  }

  getStatusText(status: string | null | undefined): string {
    const statusMap: { [key: string]: string } = {
      PENDING: 'Chờ xác nhận',
      PROCESSING: 'Đang xử lý',
      SHIPPED: 'Đang giao',
      DELIVERED: 'Đã giao',
      COMPLETED: 'Đã hoàn thành',
      CANCELLED: 'Đã hủy',
    };
    return statusMap[status || ''] || status || 'N/A';
  }

  updateStatus(order: IOrder, newStatus: string): void {
    if (!order.id) {
      this.notify.error('ID đơn hàng không hợp lệ.');
      return;
    }
    if (confirm(`Bạn có chắc chắn muốn cập nhật trạng thái đơn hàng #${order.id} thành ${this.getStatusText(newStatus)} không?`)) {
      this.orderService.updateStatus(order.id, newStatus).subscribe({
        next: (res: HttpResponse<IOrder>) => {
          if (res.body) {
            this.loadAll();
            this.notify.success(`Trạng thái đơn hàng #${res.body.id} đã được cập nhật.`);
          }
        },
        error: error => {
          console.error('Failed to update order status:', error);
          this.notify.error('Cập nhật trạng thái đơn hàng thất bại.');
        },
      });
    }
  }

  toggleOrderDetail(order: IOrder): void {
    if (this.selectedOrder()?.id === order.id) {
      this.selectedOrder.set(null);
    } else {
      this.selectedOrder.set(order);
    }
  }

  deleteOrder(orderId: number): void {
    if (confirm(`Bạn có chắc chắn muốn xóa đơn hàng #${orderId} không?`)) {
      this.orderService.delete(orderId).subscribe({
        next: () => {
          this.loadAll();
          this.notify.success(`Đơn hàng #${orderId} đã được xóa.`);
        },
        error: error => {
          console.error('Failed to delete order:', error);
          this.notify.error('Xóa đơn hàng thất bại.');
        },
      });
    }
  }

  onSearchChange(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.searchTimeout = setTimeout(() => {
      this.page = 1;
      this.loadAll();
    }, 500);
  }

  clearSearch(): void {
    this.searchOrderCode.set('');
    this.page = 1;
    this.loadAll();
  }

  transition(sortState?: SortState): void {
    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute.parent,
      queryParams: {
        page: this.page,
        sort: this.sortService.buildSortParam(sortState ?? this.sortState(), 'orderDate,desc'),
      },
    });
  }

  private handleNavigation(): void {
    combineLatest([this.activatedRoute.data, this.activatedRoute.queryParamMap]).subscribe(([data, params]) => {
      const page = params.get('page');
      this.page = +(page ?? 1);
      this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data.defaultSort));
      this.loadAll();
    });
  }

  private onSuccess(orders: IOrder[] | null, headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')));
    this.orders.set(orders);
  }
}
