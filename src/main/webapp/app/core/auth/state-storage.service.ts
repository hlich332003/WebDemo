import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class StateStorageService {
  private readonly previousUrlKey = 'previousUrl';
  private readonly authenticationKey = 'jhi-authenticationToken';

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

  /**
   * Lưu JWT token vào localStorage hoặc sessionStorage
   * @param authenticationToken - JWT token
   * @param rememberMe - true để lưu vào localStorage, false để lưu vào sessionStorage
   */
  storeAuthenticationToken(authenticationToken: string, rememberMe: boolean): void {
    if (rememberMe) {
      localStorage.setItem(this.authenticationKey, authenticationToken);
      console.log('✅ Token saved to localStorage:', authenticationToken.substring(0, 20) + '...');
    } else {
      sessionStorage.setItem(this.authenticationKey, authenticationToken);
      console.log('✅ Token saved to sessionStorage:', authenticationToken.substring(0, 20) + '...');
    }
  }

  /**
   * Lấy JWT token từ localStorage hoặc sessionStorage
   */
  getAuthenticationToken(): string | null {
    const tokenFromLocalStorage = localStorage.getItem(this.authenticationKey);
    if (tokenFromLocalStorage) {
      console.log('🔑 Token retrieved from localStorage:', tokenFromLocalStorage.substring(0, 20) + '...');
      return tokenFromLocalStorage;
    }

    const tokenFromSessionStorage = sessionStorage.getItem(this.authenticationKey);
    if (tokenFromSessionStorage) {
      console.log('🔑 Token retrieved from sessionStorage:', tokenFromSessionStorage.substring(0, 20) + '...');
      return tokenFromSessionStorage;
    }

    console.log('⚠️ No token found in localStorage or sessionStorage');
    return null;
  }

  /**
   * Xóa JWT token (khi logout)
   */
  clearAuthenticationToken(): void {
    localStorage.removeItem(this.authenticationKey);
    sessionStorage.removeItem(this.authenticationKey);
    console.log('🗑️ Token cleared from localStorage and sessionStorage');
  }
}
