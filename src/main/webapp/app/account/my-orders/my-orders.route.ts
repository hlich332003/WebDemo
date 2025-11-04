import { Route } from '@angular/router';

import MyOrdersComponent from './my-orders.component';

const myOrdersRoute: Route = {
  path: 'my-orders',
  component: MyOrdersComponent,
  data: {
    pageTitle: 'Đơn hàng của tôi',
  },
};

export default myOrdersRoute;
