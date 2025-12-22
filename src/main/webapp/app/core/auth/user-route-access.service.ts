import { inject, isDevMode } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { map } from 'rxjs/operators';

import { AccountService } from 'app/core/auth/account.service';
import { StateStorageService } from './state-storage.service';
import { LoginModalService } from 'app/core/login/login-modal.service';

export const UserRouteAccessService: CanActivateFn = (
  next: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
) => {
  const accountService = inject(AccountService);
  const router = inject(Router);
  const stateStorageService = inject(StateStorageService);
  const loginModalService = inject(LoginModalService);

  return accountService.identity().pipe(
    map((account) => {
      if (account) {
        const authorities = next.data['authorities'] ?? [];

        if (accountService.hasAnyAuthority(['ROLE_ADMIN'])) {
          // Nếu là admin, chỉ cho phép truy cập các trang admin
          if (state.url.startsWith('/admin')) {
            return true;
          }
          // Nếu cố gắng truy cập trang khác, chuyển hướng về trang admin
          router.navigate(['/admin']);
          return false;
        }

        if (
          authorities.length === 0 ||
          accountService.hasAnyAuthority(authorities)
        ) {
          return true;
        }

        if (isDevMode()) {
          console.error(
            'User does not have any of the required authorities:',
            authorities,
          );
        }
        router.navigate(['accessdenied']);
        return false;
      }

      // Lưu URL để redirect sau khi đăng nhập
      stateStorageService.storeUrl(state.url);

      // Hiển thị modal yêu cầu đăng nhập (giống Shopee)
      loginModalService.open();

      return false;
    }),
  );
};
