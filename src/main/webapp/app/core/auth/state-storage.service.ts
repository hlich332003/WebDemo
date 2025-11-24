import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class StateStorageService {
  private readonly previousUrlKey = 'previousUrl';
  private readonly authenticationKey = 'jhi-authenticationToken';
  private readonly reloadingTabKey = 'jhi-reloadingTab';
  private readonly tabCountKey = 'jhi-tab-count'; // Đếm số tab đang mở

  constructor() {
    this.initTabTracking();
  }

  /**
   * Khởi tạo tab tracking:
   * - Tăng counter khi mở tab mới
   * - Giảm counter khi đóng tab
   * - Clear localStorage khi đóng tab cuối cùng
   */
  private initTabTracking(): void {
    // Tăng tab counter khi mở tab
    const currentCount = this.getTabCount();
    this.setTabCount(currentCount + 1);

    // Lắng nghe sự kiện đóng tab/browser
    window.addEventListener('beforeunload', () => {
      const newCount = this.getTabCount() - 1;

      if (newCount <= 0) {
        // Đóng tab cuối cùng → Clear localStorage
        console.log('🚪 Đóng tab cuối cùng, xóa token khỏi localStorage');
        localStorage.removeItem(this.authenticationKey);
        localStorage.removeItem(this.tabCountKey);
      } else {
        // Còn tab khác đang mở
        this.setTabCount(newCount);
      }
    });
  }

  private getTabCount(): number {
    const count = localStorage.getItem(this.tabCountKey);
    return count ? parseInt(count, 10) : 0;
  }

  private setTabCount(count: number): void {
    localStorage.setItem(this.tabCountKey, count.toString());
  }

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
