import { Route } from '@angular/router';
import { PaymentManagementComponent } from './list/payment-management.component';
import { PaymentManagementDetailComponent } from './detail/payment-management-detail.component';
import { paymentResolve } from 'app/entities/payment/route/payment-routing-resolve.service';

export const paymentManagementRoute: Route = {
  path: 'payment-management',
  children: [
    {
      path: '',
      component: PaymentManagementComponent,
    },
    {
      path: ':id/view',
      component: PaymentManagementDetailComponent,
      resolve: {
        payment: paymentResolve,
      },
    },
  ],
};
