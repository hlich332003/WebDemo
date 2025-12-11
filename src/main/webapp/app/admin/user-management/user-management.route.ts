import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, Routes } from '@angular/router';
import { of } from 'rxjs';

import { IUser } from './user-management.model';
import { UserManagementService } from './service/user-management.service';

export const userManagementResolve: ResolveFn<IUser | null> = (
  route: ActivatedRouteSnapshot,
) => {
  const email = route.paramMap.get('email');
  if (email) {
    return inject(UserManagementService).find(email);
  }
  return of(null);
};

const userManagementRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/user-management.component'),
    data: {
      defaultSort: 'id,asc',
    },
  },
  {
    path: ':email/view',
    loadComponent: () => import('./detail/user-management-detail.component'),
    resolve: {
      user: userManagementResolve,
    },
  },
  {
    path: 'new',
    loadComponent: () => import('./update/user-management-update.component'),
    resolve: {
      user: userManagementResolve,
    },
  },
  {
    path: ':email/edit',
    loadComponent: () => import('./update/user-management-update.component'),
    resolve: {
      user: userManagementResolve,
    },
  },
];

export default userManagementRoute;
