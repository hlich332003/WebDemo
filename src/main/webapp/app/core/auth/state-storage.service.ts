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
    return previousUrl
      ? (JSON.parse(previousUrl) as string | null)
      : previousUrl;
  }

  clearUrl(): void {
    sessionStorage.removeItem(this.previousUrlKey);
  }

  /**
   * Lưu JWT token vào localStorage để persist qua page reload
   * @param authenticationToken - JWT token
   * @param _rememberMe - (Unused) Giữ lại để tương thích với interface
   */
  storeAuthenticationToken(
    authenticationToken: string,
    _rememberMe: boolean,
  ): void {
    // Luôn lưu vào localStorage để không bị mất khi refresh
    localStorage.setItem(this.authenticationKey, authenticationToken);
    console.log(
      '✅ Token saved to localStorage:',
      authenticationToken.substring(0, 20) + '...',
    );
  }

  /**
   * Lấy JWT token từ localStorage
   */
  getAuthenticationToken(): string | null {
    const token = localStorage.getItem(this.authenticationKey);
    if (token) {
      console.log(
        '🔑 Token retrieved from localStorage:',
        token.substring(0, 20) + '...',
      );
    } else {
      console.log('⚠️ No token found in localStorage');
    }
    return token;
  }

  /**
   * Xóa JWT token (khi logout)
   */
  clearAuthenticationToken(): void {
    localStorage.removeItem(this.authenticationKey);
    console.log('🗑️ Token cleared from localStorage');
  }
}
