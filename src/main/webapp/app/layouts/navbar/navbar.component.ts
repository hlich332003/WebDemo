import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import SharedModule from 'app/shared/shared.module';
import HasAnyAuthorityDirective from 'app/shared/auth/has-any-authority.directive';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { EntityNavbarItems } from 'app/entities/entity-navbar-items';
import { environment } from 'environments/environment';
import NavbarItem from './navbar-item.model';
import { CartService } from 'app/shared/services/cart.service';

@Component({
  selector: 'jhi-navbar',
  standalone: true,
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  imports: [RouterModule, SharedModule, HasAnyAuthorityDirective, FormsModule, FontAwesomeModule],
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

  // Debounce search
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  // Tối ưu: Sử dụng Observable trực tiếp từ service
  cartItemCount$: Observable<number>;

  /**
   * Computed signal to determine if the cart should be shown.
   * The cart is hidden only for users with the 'ROLE_ADMIN' authority.
   */
  showCart = computed(() => {
    const currentAccount = this.account();
    // If the user is not logged in, show the cart.
    if (!currentAccount) {
      return true;
    }
    // Hide the cart if the user is an admin.
    return !currentAccount.authorities.includes('ROLE_ADMIN');
  });

  private readonly loginService = inject(LoginService);
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);
  private readonly cartService = inject(CartService); // Inject CartService

  constructor() {
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
    // Gán Observable từ service
    this.cartItemCount$ = this.cartService.totalQuantity$;
  }

  ngOnInit(): void {
    this.entitiesNavbarItems = EntityNavbarItems;
    this.profileService.getProfileInfo().subscribe({
      next: profileInfo => {
        this.inProduction = profileInfo.inProduction;
        this.openAPIEnabled = profileInfo.openAPIEnabled;
      },
      error: () => {
        // Ignore info endpoint error
      },
    });

    // Setup debounce search
    this.searchSubject
      .pipe(
        debounceTime(200), // Chờ 200ms sau khi user ngừng gõ
        distinctUntilChanged(), // Chỉ trigger khi giá trị thay đổi
        takeUntil(this.destroy$),
      )
      .subscribe(term => {
        if (term.trim()) {
          this.isSearching = true;
          this.router.navigate(['/products'], { queryParams: { search: term.trim() } }).then(() => {
            this.isSearching = false;
          });
        }
      });
  }

  /**
   * Được gọi khi user gõ vào ô search
   */
  onSearchInput(event: Event): void {
    const term = (event.target as HTMLInputElement).value;
    this.searchSubject.next(term);
  }

  /**
   * Clear search
   */
  clearSearch(): void {
    // Clear local search state
    this.searchTerm = '';
    this.isSearching = false;
    // Emit empty to debounce stream to cancel pending searches
    this.searchSubject.next('');
    // Navigate to product list and remove the search param
    this.router.navigate(['/products'], { queryParams: { search: null }, queryParamsHandling: 'merge' });
  }

  /**
   * Submit search form
   */
  search(): void {
    if (this.searchTerm.trim()) {
      this.isSearching = true;
      this.router.navigate(['/products'], { queryParams: { search: this.searchTerm.trim() } }).then(() => {
        this.isSearching = false;
      });
    }
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
}
