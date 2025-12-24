import { Routes } from '@angular/router';
import { reviewManagementRoute } from './review-management/review-management.route';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
/* jhipster-needle-add-admin-module-import - JHipster will add admin modules imports here */

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./admin-home/admin-home.component'),
    title: 'Trang quản trị',
    canActivate: [UserRouteAccessService],
    data: {
      authorities: ['ROLE_ADMIN'],
    },
  },
  {
    path: 'user-management',
    loadChildren: () => import('./user-management/user-management.route'),
    title: 'Quản lý người dùng',
    canActivate: [UserRouteAccessService],
    data: {
      authorities: ['ROLE_ADMIN'],
    },
  },
  {
    path: 'product-management',
    loadChildren: () => import('./product-management/product-management.route'),
    title: 'Quản lý sản phẩm',
    canActivate: [UserRouteAccessService],
    data: {
      authorities: ['ROLE_ADMIN'],
    },
  },
  {
    path: 'order-management', // Thêm route cho quản lý đơn hàng
    loadChildren: () => import('./order-management/order-management.route'),
    title: 'Quản lý đơn hàng',
    canActivate: [UserRouteAccessService],
    data: {
      authorities: ['ROLE_ADMIN'],
    },
  },
  {
    path: 'review-management',
    ...reviewManagementRoute,
    title: 'Quản lý đánh giá',
    canActivate: [UserRouteAccessService],
    data: {
      authorities: ['ROLE_ADMIN'],
    },
  },
  {
    path: 'customer-support',
    loadComponent: () => import('./customer-support/customer-support.component').then(m => m.CustomerSupportComponent),
    title: 'Chăm sóc khách hàng',
    canActivate: [UserRouteAccessService],
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_CSKH'],
    },
  },
  {
    path: 'docs',
    loadComponent: () => import('./docs/docs.component'),
    title: 'API Docs',
    canActivate: [UserRouteAccessService],
    data: {
      authorities: ['ROLE_ADMIN'],
    },
  },
  /* jhipster-needle-add-admin-route - JHipster will add admin routes here */
];

export default routes;
