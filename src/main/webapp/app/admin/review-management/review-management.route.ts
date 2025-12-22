import { Route } from '@angular/router';
import { ReviewManagementComponent } from './list/review-management.component';
import { ReviewManagementDetailComponent } from './detail/review-management-detail.component';
import { reviewResolve } from 'app/entities/review/route/review-routing-resolve.service';

export const reviewManagementRoute: Route = {
  path: 'review-management',
  children: [
    {
      path: '',
      component: ReviewManagementComponent,
    },
    {
      path: ':id/view',
      component: ReviewManagementDetailComponent,
      resolve: {
        review: reviewResolve,
      },
    },
  ],
};
