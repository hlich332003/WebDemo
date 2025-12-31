import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms'; // Import FormsModule

import { OrderService } from 'app/entities/order/order.service'; // Sử dụng OrderService chung
import { IOrder } from 'app/admin/order-management/order.model'; // Sử dụng model Order của admin
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';

@Component({
  selector: 'jhi-my-order-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule, FormsModule], // Thêm FormsModule
  templateUrl: './my-order-detail.component.html',
  styleUrls: ['./my-order-detail.component.scss'],
})
export class MyOrderDetailComponent implements OnInit {
  order: IOrder | null = null; // Đã đổi kiểu IOrder thành any
  isLoading = false;
  isEditingAddress = false; // Biến để kiểm soát trạng thái chỉnh sửa địa chỉ
  newDeliveryAddress = ''; // Biến để lưu địa chỉ mới

  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);
  private utils = inject(UtilsService);
  private notify = inject(NotificationService);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const orderId = params.get('id');
      if (orderId) {
        this.loadOrder(Number(orderId));
      }
    });
  }

  loadOrder(id: number): void {
    this.isLoading = true;
    this.orderService.find(id).subscribe({
      next: (res: HttpResponse<IOrder>) => {
        // Đã đổi kiểu IOrder thành any
        this.order = res.body;
        this.isLoading = false;
        this.newDeliveryAddress = this.order?.deliveryAddress ?? ''; // Khởi tạo địa chỉ mới
      },
      error: () => {
        this.isLoading = false;
        this.notify.error('Không thể tải chi tiết đơn hàng.');
      },
    });
  }

  cancelOrder(orderId: number): void {
    if (confirm('Bạn có chắc chắn muốn hủy đơn hàng này không?')) {
      this.orderService.cancelOrder(orderId).subscribe({
        next: (res: HttpResponse<IOrder>) => {
          // Đã đổi kiểu IOrder thành any
          if (res.body) {
            this.order = res.body;
            this.notify.success('Đơn hàng đã được hủy thành công.');
          }
        },
        error: () => {
          this.notify.error('Không thể hủy đơn hàng.');
        },
      });
    }
  }

  toggleEditAddress(): void {
    this.isEditingAddress = !this.isEditingAddress;
    if (!this.isEditingAddress) {
      this.newDeliveryAddress = this.order?.deliveryAddress ?? ''; // Reset nếu hủy chỉnh sửa
    }
  }

  saveDeliveryAddress(orderId: number): void {
    if (!this.newDeliveryAddress.trim()) {
      this.notify.error('Địa chỉ giao hàng không được để trống.');
      return;
    }
    this.orderService.updateDeliveryAddress(orderId, this.newDeliveryAddress).subscribe({
      next: (res: HttpResponse<IOrder>) => {
        if (res.body) {
          this.order = res.body;
          this.notify.success('Địa chỉ giao hàng đã được cập nhật.');
          this.isEditingAddress = false;
        }
      },
      error: () => {
        this.notify.error('Không thể cập nhật địa chỉ giao hàng.');
      },
    });
  }

  formatPrice(price: number | null | undefined): string {
    if (price === null || price === undefined) {
      return this.utils.formatPrice(0);
    }
    return this.utils.formatPrice(price);
  }

  formatOrderDate(date: Date | null | undefined): string {
    if (!date) {
      return 'N/A';
    }
    const options: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    };
    return new Date(date).toLocaleDateString('vi-VN', options);
  }

  getStatusText(status: string | null | undefined): string {
    if (!status) return 'Không xác định';

    const statusMap: Record<string, string> = {
      PENDING: 'Chờ xác nhận',
      PROCESSING: 'Đang xử lý',
      SHIPPED: 'Đang giao hàng',
      DELIVERED: 'Đã giao hàng',
      COMPLETED: 'Đã hoàn thành',
      CANCELLED: 'Đã hủy',
    };

    return statusMap[status.toUpperCase()] ?? status;
  }

  previousState(): void {
    window.history.back();
  }
}
