import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { IOrder } from 'app/admin/order-management/order.model';
import { MyOrdersService } from './my-orders.service';

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
      error: () => {
        this.isLoading = false;
        // Xử lý lỗi
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
}
