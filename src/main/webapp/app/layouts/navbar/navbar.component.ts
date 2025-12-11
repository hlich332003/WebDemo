import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
} from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Observable, Subject } from 'rxjs';
import {
  debounceTime,
  distinctUntilChanged,
  takeUntil,
  map,
} from 'rxjs/operators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgOptimizedImage } from '@angular/common';

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
    NgOptimizedImage, // Add NgOptimizedImage here
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

  cartItemCount$: Observable<number>;
  wishlistItemCount$: Observable<number>;

  showCart = computed(() => {
    const currentAccount = this.account();
    if (!currentAccount) {
      return true;
    }
    return !currentAccount.authorities.includes('ROLE_ADMIN');
  });

  private readonly loginService = inject(LoginService);
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);
  private readonly cartService = inject(CartService);
  private readonly wishlistService = inject(WishlistService);

  constructor() {
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v')
        ? VERSION
        : `v${VERSION}`;
    }
    this.cartItemCount$ = this.cartService.totalQuantity$;
    this.wishlistItemCount$ = this.wishlistService.count$;
  }

  ngOnInit(): void {
    this.entitiesNavbarItems = EntityNavbarItems;
    this.profileService.getProfileInfo().subscribe({
      next: (profileInfo) => {
        this.inProduction = profileInfo.inProduction;
        this.openAPIEnabled = profileInfo.openAPIEnabled;
      },
      error: () => {
        // Ignore
      },
    });

    this.searchSubject
      .pipe(debounceTime(200), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((term) => {
        if (term.trim()) {
          this.isSearching = true;
          this.router
            .navigate(['/products'], { queryParams: { search: term.trim() } })
            .then(() => {
              this.isSearching = false;
            });
        }
      });
  }

  onSearchInput(event: Event): void {
    const term = (event.target as HTMLInputElement).value;
    this.searchSubject.next(term);
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.isSearching = false;
    this.searchSubject.next('');
    this.router.navigate(['/products'], {
      queryParams: { search: null },
      queryParamsHandling: 'merge',
    });
  }

  search(): void {
    if (this.searchTerm.trim()) {
      this.isSearching = true;
      this.router
        .navigate(['/products'], {
          queryParams: { search: this.searchTerm.trim() },
        })
        .then(() => {
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
    this.isNavbarCollapsed.update((isNavbarCollapsed) => !isNavbarCollapsed);
  }
}
