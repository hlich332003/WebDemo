import { Routes } from '@angular/router';
import { Authority } from 'app/config/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { errorRoute } from './layouts/error/error.route';

const routes: Routes = [
  // Trang chủ
  {
    path: '',
    loadComponent: () => import('./home/home.component').then(m => m.HomeComponent),
    title: 'Trang chủ',
  },

  // Navbar outlet
  {
    path: '',
    loadComponent: () => import('./layouts/navbar/navbar.component').then(m => m.NavbarComponent),
    outlet: 'navbar',
  },

  // Trang admin (yêu cầu quyền ADMIN)
  {
    path: 'admin',
    data: { authorities: [Authority.ADMIN] },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./admin/admin.routes'),
  },

  // Trang tài khoản
  {
    path: 'account',
    loadChildren: () => import('./account/account.route'),
  },

  // Trang đăng nhập
  {
    path: 'login',
    loadComponent: () => import('./login/login.component').then(m => m.LoginComponent),
    title: 'Đăng nhập',
  },

  // Các entity khác
  {
    path: '',
    loadChildren: () => import('./entities/entity.routes'),
  },

  // Trang danh sách sản phẩm
  {
    path: 'products',
    loadComponent: () => import('./product-list/product-list.component').then(m => m.ProductListComponent),
    title: 'Danh sách sản phẩm',
  },

  // Trang chi tiết sản phẩm
  {
    path: 'product/:id',
    loadComponent: () => import('./product-detail/product-detail.component').then(m => m.ProductDetailComponent),
    title: 'Chi tiết sản phẩm',
  },

  // Trang giỏ hàng
  {
    path: 'cart',
    loadComponent: () => import('./cart/cart.component').then(m => m.CartComponent),
    title: 'Giỏ hàng',
  },

  // Trang thanh toán
  {
    path: 'checkout',
    loadComponent: () => import('./checkout/checkout.component').then(m => m.CheckoutComponent),
    title: 'Thanh toán',
  },

  // Các route lỗi
  ...errorRoute,
];

export default routes;
