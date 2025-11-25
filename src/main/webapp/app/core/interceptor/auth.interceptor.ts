import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { StateStorageService } from 'app/core/auth/state-storage.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const stateStorageService = inject(StateStorageService);
  const token = stateStorageService.getAuthenticationToken();

  // Tự động thêm JWT token vào header
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    console.log(`[HTTP] ${req.method} ${req.url} - Token attached ✅`);
  } else {
    console.log(`[HTTP] ${req.method} ${req.url} - No token ⚠️`);
  }

  return next(req);
};
