import { Routes } from '@angular/router';
/* jhipster-needle-add-admin-module-import - JHipster will add admin modules imports here */

const routes: Routes = [
  {
    path: 'user-management',
    loadChildren: () => import('./user-management/user-management.route'),
    title: 'userManagement.home.title',
  },
  {
    path: 'product-management',
    loadChildren: () => import('./product-management/product-management.route'),
    title: 'Quản lý sản phẩm',
  },
  {
    path: 'order-management', // Thêm route cho quản lý đơn hàng
    loadChildren: () => import('./order-management/order-management.route'),
    title: 'Quản lý đơn hàng',
  },
  {
    path: 'import', // Thêm route cho import
    loadComponent: () => import('./import/import.component').then(m => m.ImportComponent),
    title: 'Import Dữ liệu',
  },
  {
    path: 'docs',
    loadComponent: () => import('./docs/docs.component'),
    title: 'global.menu.admin.apidocs',
  },
  /* jhipster-needle-add-admin-route - JHipster will add admin routes here */
];

export default routes;
