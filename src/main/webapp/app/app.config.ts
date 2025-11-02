import { ApplicationConfig, LOCALE_ID, importProvidersFrom, inject } from '@angular/core';
import { BrowserModule, Title } from '@angular/platform-browser';
import {
  NavigationError,
  Router,
  RouterFeatures,
  TitleStrategy,
  provideRouter,
  withComponentInputBinding,
  withDebugTracing,
  withNavigationErrorHandler,
} from '@angular/router';
import { ServiceWorkerModule } from '@angular/service-worker';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { NgbDateAdapter } from '@ng-bootstrap/ng-bootstrap';

import './config/dayjs';
import { environment } from 'environments/environment';
import { httpInterceptorProviders } from './core/interceptor';
import routes from './app.routes';
import { NgbDateDayjsAdapter } from './config/datepicker-adapter';
import { AppPageTitleStrategy } from './app-page-title-strategy';

// 👉 Cấu hình chung (từ config.js cũ)
export const CONFIG = {
  CART_STORAGE_KEY: 'shopapp_cart',
  PRODUCTS_STORAGE_KEY: 'shopapp_products',
  THEME: {
    primary: '#004080',
    secondary: '#ff6b6b',
    success: '#28a745',
    danger: '#dc3545',
  },
};

const routerFeatures: RouterFeatures[] = [
  withComponentInputBinding(),
  withNavigationErrorHandler((e: NavigationError) => {
    const router = inject(Router);
    if (e.error.status === 403) {
      router.navigate(['/accessdenied']);
    } else if (e.error.status === 404) {
      router.navigate(['/404']);
    } else if (e.error.status === 401) {
      router.navigate(['/login']);
    } else {
      router.navigate(['/error']);
    }
  }),
];

if (environment.DEBUG_INFO_ENABLED) {
  routerFeatures.push(withDebugTracing());
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, ...routerFeatures),
    importProvidersFrom(BrowserModule),
    importProvidersFrom(ServiceWorkerModule.register('ngsw-worker.js', { enabled: false })),
    provideHttpClient(withInterceptorsFromDi()),
    Title,
    { provide: LOCALE_ID, useValue: 'en' },
    { provide: NgbDateAdapter, useClass: NgbDateDayjsAdapter },
    httpInterceptorProviders,
    { provide: TitleStrategy, useClass: AppPageTitleStrategy },
    // jhipster-needle-angular-add-module JHipster will add new module here
  ],
};
