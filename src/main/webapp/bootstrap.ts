import { enableProdMode } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import AppComponent from './app/app.component';

import { environment } from './environments/environment';

// disable debug data on prod profile to improve performance
if (!environment.DEBUG_INFO_ENABLED) {
  enableProdMode();
}

// Add global error handlers to prevent app crashes
window.addEventListener('unhandledrejection', event => {
  console.warn('Unhandled promise rejection:', event.reason);
  event.preventDefault(); // Prevent the default browser action
});

window.addEventListener('error', event => {
  console.warn('Global error caught:', event.error ?? event.message);
  // Don't prevent default for actual errors, just log them
});

bootstrapApplication(AppComponent, appConfig)
  .then(() => {
    // eslint-disable-next-line no-console
    console.log('Application started');

    // Register Service Worker for PWA
    if ('serviceWorker' in navigator && environment.production) {
      navigator.serviceWorker
        .register('/ngsw-worker.js')
        .then(registration => {
          // eslint-disable-next-line no-console
          console.log('ServiceWorker registration successful:', registration.scope);
        })
        .catch((err: unknown) => {
          console.error('ServiceWorker registration failed:', err);
        });
    }
  })
  .catch((err: unknown) => console.error(err));
