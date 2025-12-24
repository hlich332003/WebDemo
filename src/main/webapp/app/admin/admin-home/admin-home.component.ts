import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import SharedModule from 'app/shared/shared.module';
import { ApplicationConfigService } from 'app/core/config/application-config.service';

export interface DashboardStats {
  totalRevenue: number;
  totalOrders: number;
  totalCustomers: number;
  totalProducts: number;
}

@Component({
  selector: 'jhi-admin-home',
  standalone: true,
  templateUrl: './admin-home.component.html',
  styleUrls: ['./admin-home.component.scss'],
  imports: [CommonModule, RouterModule, SharedModule],
  providers: [CurrencyPipe],
})
export default class AdminHomeComponent implements OnInit {
  account = signal<Account | null>(null);
  stats = signal<DashboardStats | null>(null);
  isCSKH = false;

  private readonly accountService = inject(AccountService);
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly currencyPipe = inject(CurrencyPipe);

  ngOnInit(): void {
    this.accountService.getAuthenticationState().subscribe(account => {
      this.account.set(account);
      this.isCSKH = this.accountService.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_CSKH']);

      // If this is admin/CSKH, signal that admin layout is ready for WS connection
      if (this.isCSKH) {
        // small microtask to ensure view init
        Promise.resolve().then(() => this.accountService.signalAdminReady());
      }
    });
    this.loadStats();
  }

  loadStats(): void {
    this.getDashboardStats().subscribe({
      next: stats => this.stats.set(stats),
      error: () => {
        this.stats.set({
          totalRevenue: 0,
          totalOrders: 0,
          totalCustomers: 0,
          totalProducts: 0,
        });
      },
    });
  }

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(this.applicationConfigService.getEndpointFor('api/admin/dashboard-stats'));
  }

  formatRevenue(revenue: number | null | undefined): string {
    const value = revenue ?? 0;
    return this.currencyPipe.transform(value, 'VND', 'symbol', '1.0-0') ?? '0 ₫';
  }
}
