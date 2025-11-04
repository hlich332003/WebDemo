import { Component, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import SharedModule from 'app/shared/shared.module';
import { OrderService } from 'app/entities/order/order.service';
import { HttpResponse } from '@angular/common/http';

@Component({
  selector: 'jhi-my-orders',
  standalone: true,
  imports: [SharedModule, RouterModule],
  templateUrl: './my-orders.component.html',
  styleUrls: ['./my-orders.component.scss'],
})
export default class MyOrdersComponent implements OnInit {
  orders: any[] = [];
  isLoading = false;

  private orderService = inject(OrderService);

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.isLoading = true;
    this.orderService.queryMyOrders().subscribe({
      next: (res: HttpResponse<any[]>) => {
        this.isLoading = false;
        this.orders = res.body ?? [];
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  trackId(index: number, item: any): number {
    return item.id;
  }
}
