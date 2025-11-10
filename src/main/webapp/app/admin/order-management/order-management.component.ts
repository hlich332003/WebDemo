import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { combineLatest } from 'rxjs';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

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
  imports: [CommonModule, RouterModule, FontAwesomeModule, SortDirective, SortByDirective, ItemCountComponent, NgbPaginationModule],
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

  private orderManagementService = inject(OrderManagementService);
  private notify = inject(NotificationService);
  private activatedRoute = inject(ActivatedRoute);
  private router = inject(Router);
  private sortService = inject(SortService);

  ngOnInit(): void {
    this.handleNavigation();
  }

  loadAll(): void {
    this.isLoading.set(true);
    this.orderManagementService
      .query({
        page: this.page - 1,
        size: this.itemsPerPage,
        sort: this.sortService.buildSortParam(this.sortState(), 'orderDate,desc'),
      })
      .subscribe({
        next: (res: HttpResponse<IOrder[]>) => {
          this.isLoading.set(false);
          this.onSuccess(res.body, res.headers);
        },
        error: () => {
          this.isLoading.set(false);
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

  updateStatus(order: IOrder, newStatus: string): void {
    if (!order.id) {
      return;
    }
    if (confirm(`Bạn có chắc chắn muốn cập nhật trạng thái đơn hàng #${order.id} thành ${newStatus} không?`)) {
      this.orderManagementService.updateStatus(order.id, newStatus).subscribe({
        next: (res: HttpResponse<IOrder>) => {
          if (res.body) {
            this.loadAll(); // Tải lại danh sách sau khi cập nhật
            this.notify.success(`Trạng thái đơn hàng #${res.body.id} đã được cập nhật.`);
          }
        },
        error: () => {
          this.notify.error('Cập nhật trạng thái đơn hàng thất bại.');
        },
      });
    }
  }

  deleteOrder(orderId: number): void {
    if (confirm(`Bạn có chắc chắn muốn xóa đơn hàng #${orderId} không?`)) {
      this.orderManagementService.delete(orderId).subscribe({
        next: () => {
          this.loadAll(); // Tải lại danh sách sau khi xóa
          this.notify.success(`Đơn hàng #${orderId} đã được xóa.`);
        },
        error: () => {
          this.notify.error('Xóa đơn hàng thất bại.');
        },
      });
    }
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
