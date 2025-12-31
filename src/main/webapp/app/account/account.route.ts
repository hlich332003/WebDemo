import { Routes } from '@angular/router';

import activateRoute from './activate/activate.route';
import passwordRoute from './password/password.route';
import passwordResetFinishRoute from './password-reset/finish/password-reset-finish.route';
import passwordResetInitRoute from './password-reset/init/password-reset-init.route';
import registerRoute from './register/register.route';
import settingsRoute from './settings/settings.route';
import myOrdersRoute from './my-orders/my-orders.route';
import { MyOrderDetailComponent } from './my-orders/detail/my-order-detail.component';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const accountRoutes: Routes = [
  activateRoute,
  passwordRoute,
  passwordResetFinishRoute,
  passwordResetInitRoute,
  registerRoute,
  settingsRoute,
  {
    path: 'my-orders/:id',
    component: MyOrderDetailComponent,
    canActivate: [UserRouteAccessService],
    data: {
      pageTitle: 'Chi tiết đơn hàng của tôi',
      authorities: ['ROLE_USER'],
    },
  },
  ...myOrdersRoute,
];

export default accountRoutes;
