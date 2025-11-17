import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class StateStorageService {
  private readonly previousUrlKey = 'previousUrl';
  private readonly authenticationKey = 'jhi-authenticationToken';
  private readonly reloadingTabKey = 'jhi-reloadingTab'; // New key for tracking tab reload

  storeUrl(url: string): void {
    sessionStorage.setItem(this.previousUrlKey, JSON.stringify(url));
  }

  getUrl(): string | null {
    const previousUrl = sessionStorage.getItem(this.previousUrlKey);
    return previousUrl ? (JSON.parse(previousUrl) as string | null) : previousUrl;
  }

  clearUrl(): void {
    sessionStorage.removeItem(this.previousUrlKey);
  }

  storeAuthenticationToken(authenticationToken: string, rememberMe: boolean): void {
    this.clearAuthenticationToken();
    // Lưu vào localStorage để chia sẻ giữa các tab
    localStorage.setItem(this.authenticationKey, authenticationToken);
    // Đánh dấu có ít nhất 1 tab đang mở
    sessionStorage.setItem('tab-active', 'true');
  }

  getAuthenticationToken(): string | null {
    return localStorage.getItem(this.authenticationKey) ?? sessionStorage.getItem(this.authenticationKey);
  }

  clearAuthenticationToken(): void {
    sessionStorage.removeItem(this.authenticationKey);
    localStorage.removeItem(this.authenticationKey);
  }

  // New methods to manage the reloadingTabKey
  setReloadingFlag(): void {
    sessionStorage.setItem(this.reloadingTabKey, 'true');
  }

  clearReloadingFlag(): void {
    sessionStorage.removeItem(this.reloadingTabKey);
  }

  isReloading(): boolean {
    return sessionStorage.getItem(this.reloadingTabKey) === 'true';
  }
}
