import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { IOrder } from 'app/admin/order-management/order.model';
import { MyOrdersService } from './my-orders.service';
import { OrderService } from 'app/entities/order/order.service';
import { NotificationService } from 'app/shared/notification/notification.service';

@Component({
  selector: 'jhi-my-orders',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule],
  templateUrl: './my-orders.component.html',
  styleUrls: ['./my-orders.component.scss'],
})
export class MyOrdersComponent implements OnInit {
  orders: IOrder[] = [];
  isLoading = false;

  private myOrdersService = inject(MyOrdersService);
  private orderService = inject(OrderService);
  private notify = inject(NotificationService);

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.isLoading = true;
    this.myOrdersService.query().subscribe({
      next: (res: HttpResponse<IOrder[]>) => {
        this.orders = res.body ?? [];
        this.isLoading = false;
      },
      error: error => {
        console.error('Failed to load orders:', error);
        this.isLoading = false;
        this.orders = [];
      },
    });
  }

  trackId(index: number, item: IOrder): number {
    return item.id ?? 0; // Đã sửa lỗi: Thêm ?? 0
  }

  formatOrderDate(date: Date | null | undefined): string {
    if (!date) {
      return 'N/A';
    }
    const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' };
    return new Date(date).toLocaleDateString('vi-VN', options);
  }

  cancelOrder(orderId: number): void {
    if (confirm('Bạn có chắc chắn muốn hủy đơn hàng này không?')) {
      this.orderService.cancelOrder(orderId).subscribe({
        next: () => {
          this.notify.success('Đơn hàng đã được hủy thành công.');
          this.loadAll();
        },
        error: () => {
          this.notify.error('Không thể hủy đơn hàng.');
        },
      });
    }
  }

  getStatusText(status: string | null | undefined): string {
    const statusMap: { [key: string]: string } = {
      PENDING: 'Chờ xử lý',
      PROCESSING: 'Đang xử lý',
      SHIPPED: 'Đang giao',
      DELIVERED: 'Đã giao',
      CANCELLED: 'Đã hủy',
    };
    return statusMap[status || ''] || status || 'N/A';
  }
}
