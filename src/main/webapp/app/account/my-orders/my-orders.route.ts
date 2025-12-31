import { Routes } from '@angular/router';
import { MyOrdersComponent } from './my-orders.component';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const myOrdersRoute: Routes = [
  {
    path: 'my-orders',
    component: MyOrdersComponent,
    canActivate: [UserRouteAccessService],
    data: {
      pageTitle: 'Đơn hàng của tôi',
      authorities: ['ROLE_USER'],
    },
  },
];

export default myOrdersRoute;
