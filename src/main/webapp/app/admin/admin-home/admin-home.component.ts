import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
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
}

@Component({
  selector: 'jhi-admin-home',
  standalone: true,
  templateUrl: './admin-home.component.html',
  styleUrls: ['./admin-home.component.scss'],
  imports: [CommonModule, RouterModule, SharedModule],
})
export default class AdminHomeComponent implements OnInit {
  account = signal<Account | null>(null);
  stats = signal<DashboardStats | null>(null);

  private readonly accountService = inject(AccountService);
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  ngOnInit(): void {
    this.accountService.getAuthenticationState().subscribe(account => {
      this.account.set(account);
    });
    this.loadStats();
  }

  loadStats(): void {
    this.getDashboardStats().subscribe(stats => {
      this.stats.set(stats);
    });
  }

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(this.applicationConfigService.getEndpointFor('api/admin/dashboard-stats'));
  }
}
