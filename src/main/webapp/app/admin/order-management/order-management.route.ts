import { Routes } from '@angular/router';

const orderManagementRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./order-management.component').then(
        (m) => m.OrderManagementComponent,
      ),
    data: { pageTitle: 'Quản lý đơn hàng' },
  },
];

export default orderManagementRoutes;
