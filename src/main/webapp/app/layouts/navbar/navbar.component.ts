import { Component, OnInit, OnDestroy, inject, signal, computed, ChangeDetectorRef } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Observable, Subject, of } from 'rxjs';
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
  inProduction?: boolean;
  isNavbarCollapsed = signal(true);
  openAPIEnabled?: boolean;
  version = '';
  account = inject(AccountService).trackCurrentAccount();
  entitiesNavbarItems: NavbarItem[] = [];
  searchTerm = '';
  isSearching = false;

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  public cartService = inject(CartService);
  public wishlistService = inject(WishlistService);
  public webSocketService = inject(WebSocketService);
  private cdr = inject(ChangeDetectorRef);
  private accountService = inject(AccountService);

  cartItemCount = 0;
  wishlistItemCount = 0;

  // Subscribe trực tiếp từ Service
  notifications$: Observable<INotification[]> = this.webSocketService.notifications$;
  unreadCount$: Observable<number> = this.webSocketService.unreadCount$;

  showCart = computed(() => {
    const currentAccount = this.account();
    return !currentAccount?.authorities.includes('ROLE_ADMIN');
  });

  private readonly loginService = inject(LoginService);
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);

  constructor() {
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
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

  markAsRead(id: number | undefined): void {
    // Todo: Implement mark as read API call
  }

  markAllAsRead(): void {
    // Todo: Implement mark all as read API call
  }
}
