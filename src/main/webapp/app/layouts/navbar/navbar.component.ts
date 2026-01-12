import { Component, OnInit, OnDestroy, inject, signal, computed, ChangeDetectorRef } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgOptimizedImage } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import SharedModule from 'app/shared/shared.module';
import HasAnyAuthorityDirective from 'app/shared/auth/has-any-authority.directive';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { EntityNavbarItems } from 'app/entities/entity-navbar-items';
import { environment } from 'environments/environment';
import NavbarItem from './navbar-item.model';
import { CartService } from 'app/shared/services/cart.service';
import { WishlistService } from 'app/shared/services/wishlist.service';
import { WebSocketService } from 'app/shared/services/websocket.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { INotification } from 'app/shared/model/notification.model';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { NotificationDatePipe } from 'app/shared/date/notification-date.pipe';

@Component({
  selector: 'jhi-navbar',
  standalone: true,
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  imports: [
    RouterModule,
    SharedModule,
    HasAnyAuthorityDirective,
    FormsModule,
    FontAwesomeModule,
    NgOptimizedImage,
    FormatMediumDatetimePipe,
    NotificationDatePipe,
    TranslateModule,
  ],
})
export class NavbarComponent implements OnInit, OnDestroy {
  // Public fields
  inProduction?: boolean;
  openAPIEnabled?: boolean;
  version = '';
  entitiesNavbarItems: NavbarItem[] = [];
  searchTerm = '';
  isSearching = false;
  cartItemCount = 0;
  wishlistItemCount = 0;

  // Signals and computed
  isNavbarCollapsed = signal(true);
  account = inject(AccountService).trackCurrentAccount();

  showCart = computed(() => {
    const currentAccount = this.account();
    return !currentAccount?.authorities.includes('ROLE_ADMIN');
  });

  // Injected services (public for template access)
  cartService = inject(CartService);
  wishlistService = inject(WishlistService);
  webSocketService = inject(WebSocketService);
  notificationService = inject(NotificationService);

  // Observables (initialized after services)
  notifications$: Observable<INotification[]>;
  unreadCount$: Observable<number>;

  // Private fields
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();
  private cdr = inject(ChangeDetectorRef);
  private accountService = inject(AccountService);
  private readonly loginService = inject(LoginService);
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);

  constructor() {
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
    // Initialize observables after services are injected
    this.notifications$ = this.webSocketService.notifications$;
    this.unreadCount$ = this.webSocketService.unreadCount$;
  }

  ngOnInit(): void {
    this.entitiesNavbarItems = EntityNavbarItems;
    this.profileService.getProfileInfo().subscribe({
      next: profileInfo => {
        this.inProduction = profileInfo.inProduction;
        this.openAPIEnabled = profileInfo.openAPIEnabled;
      },
    });

    // Navbar KHÔNG quản lý kết nối WebSocket nữa.
    // Việc kết nối đã được chuyển sang MainComponent (Singleton).

    this.wishlistService.count$.pipe(takeUntil(this.destroy$)).subscribe(count => {
      this.wishlistItemCount = count;
      this.cdr.detectChanges();
    });

    this.cartService.totalQuantity$.pipe(takeUntil(this.destroy$)).subscribe(count => {
      this.cartItemCount = count;
      this.cdr.detectChanges();
    });

    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe(term => {
      this.isSearching = true;
      this.router
        .navigate(['/products'], {
          queryParams: { search: term.trim() || null },
        })
        .finally(() => {
          this.isSearching = false;
        });
    });

    // Load initial notifications and unread count from DB when user is logged in
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        if (account) {
          this.loadNotifications();
          this.loadUnreadCount();
        }
      });
  }

  loadNotifications(): void {
    this.notificationService.query().subscribe({
      next: notifications => {
        // Update WebSocketService state with loaded notifications
        (this.webSocketService as any).notificationsSubject?.next(notifications);
      },
      error(err) {
        console.error('Error loading notifications:', err);
      },
    });
  }

  loadUnreadCount(): void {
    this.notificationService.getUnreadCount().subscribe({
      next: count => {
        // Update WebSocketService unread count
        (this.webSocketService as any).unreadCountSubject?.next(count);
      },
      error(err) {
        console.error('Error loading unread count:', err);
      },
    });
  }

  onSearchInput(event: Event): void {
    const term = (event.target as HTMLInputElement).value;
    this.searchSubject.next(term);
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.searchSubject.next('');
  }

  search(): void {
    this.searchSubject.next(this.searchTerm);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  collapseNavbar(): void {
    this.isNavbarCollapsed.set(true);
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate(['']);
  }

  toggleNavbar(): void {
    this.isNavbarCollapsed.update(isNavbarCollapsed => !isNavbarCollapsed);
  }

  markAsRead(notification: INotification | undefined): void {
    if (!notification?.id) return;

    // Navigate to the link if exists
    if (notification.link) {
      this.router.navigate([notification.link]);
      this.collapseNavbar(); // Close navbar on mobile
    }

    // Call API to mark as read on server
    this.notificationService.markAsRead(notification.id).subscribe(() => {
      // Update local state
      const currentNotifications = (this.webSocketService as any).notificationsSubject?.getValue() ?? [];
      const updatedNotifications = currentNotifications.map((n: INotification) => (n.id === notification.id ? { ...n, read: true } : n));

      // Update the subject
      if ((this.webSocketService as any).notificationsSubject) {
        (this.webSocketService as any).notificationsSubject.next(updatedNotifications);
      }

      // Update unread count
      const unreadCount = updatedNotifications.filter((n: INotification) => !n.read).length;
      if ((this.webSocketService as any).unreadCountSubject) {
        (this.webSocketService as any).unreadCountSubject.next(unreadCount);
      }
    });
  }

  markAllAsRead(): void {
    // Call API to mark all as read on server
    this.notificationService.markAllAsRead().subscribe(() => {
      // Update local state
      const currentNotifications = (this.webSocketService as any).notificationsSubject?.getValue() ?? [];
      const updatedNotifications = currentNotifications.map((n: INotification) => ({ ...n, read: true }));

      // Update the subject
      if ((this.webSocketService as any).notificationsSubject) {
        (this.webSocketService as any).notificationsSubject.next(updatedNotifications);
      }

      // Update unread count to 0
      if ((this.webSocketService as any).unreadCountSubject) {
        (this.webSocketService as any).unreadCountSubject.next(0);
      }
    });
  }

  // Delete a single notification
  deleteNotification(event: Event, notification: INotification): void {
    event.stopPropagation(); // Prevent marking as read
    if (!notification.id) return;

    this.notificationService.delete(notification.id).subscribe({
      next: () => {
        // Remove notification from local state
        const currentNotifications = (this.webSocketService as any).notificationsSubject?.getValue() ?? [];
        const updatedNotifications = currentNotifications.filter((n: INotification) => n.id !== notification.id);

        if ((this.webSocketService as any).notificationsSubject) {
          (this.webSocketService as any).notificationsSubject.next(updatedNotifications);
        }

        // Update unread count
        const unreadCount = updatedNotifications.filter((n: INotification) => !n.read).length;
        if ((this.webSocketService as any).unreadCountSubject) {
          (this.webSocketService as any).unreadCountSubject.next(unreadCount);
        }
      },
      error(err) {
        console.error('Error deleting notification:', err);
      },
    });
  }

  // Delete only read notifications
  deleteReadNotifications(): void {
    const currentNotifications = (this.webSocketService as any).notificationsSubject?.getValue() ?? [];
    const readNotifications = currentNotifications.filter((n: INotification) => n.read);

    if (readNotifications.length === 0) {
      alert('Không có thông báo đã đọc nào để xóa!');
      return;
    }

    if (!confirm(`Bạn có chắc chắn muốn xóa ${readNotifications.length} thông báo đã đọc không?`)) {
      return;
    }

    this.notificationService.deleteReadNotifications().subscribe({
      next: () => {
        // Keep only unread notifications in local state
        const unreadNotifications = currentNotifications.filter((n: INotification) => !n.read);
        if ((this.webSocketService as any).notificationsSubject) {
          (this.webSocketService as any).notificationsSubject.next(unreadNotifications);
        }

        // Unread count stays the same (only deleted read ones)
        // No need to update unread count
      },
      error(err) {
        console.error('Error deleting read notifications:', err);
        alert('Có lỗi xảy ra khi xóa thông báo!');
      },
    });
  }
}
