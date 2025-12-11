import { Routes } from '@angular/router';

import ProductManagementComponent from './list/product-management.component';
// import ProductManagementDetailComponent from './detail/product-detail.component'; // Không cần nữa
import ProductManagementUpdateComponent from './update/product-update.component';
import { productResolve } from 'app/entities/product/route/product-routing-resolve.service'; // Đã sửa import

const productManagementRoutes: Routes = [
  {
    path: '',
    component: ProductManagementComponent,
    data: {
      defaultSort: 'id,asc',
    },
  },
  {
    path: 'new',
    component: ProductManagementUpdateComponent,
    resolve: { product: productResolve },
    data: {
      pageTitle: 'Tạo sản phẩm mới',
    },
  },
  {
    path: ':id/edit',
    component: ProductManagementUpdateComponent,
    resolve: { product: productResolve },
    data: {
      pageTitle: 'Sửa sản phẩm',
      isEditing: true, // Thêm cờ để component biết đang ở chế độ sửa
    },
  },
  {
    path: ':id/view',
    component: ProductManagementUpdateComponent, // Trỏ đến component update
    resolve: { product: productResolve },
    data: {
      pageTitle: 'Xem sản phẩm',
      isEditing: false, // Thêm cờ để component biết đang ở chế độ xem
    },
  },
];

export default productManagementRoutes;
