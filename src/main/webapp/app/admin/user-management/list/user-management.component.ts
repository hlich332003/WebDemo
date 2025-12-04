import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

import SharedModule from 'app/shared/shared.module';
import { SortByDirective, SortDirective, SortService, sortStateSignal } from 'app/shared/sort';
import { AccountService } from 'app/core/auth/account.service';
import { UserManagementService } from '../service/user-management.service';
import { User } from '../user-management.model';
import ItemCountComponent from 'app/shared/pagination/item-count.component';

@Component({
  selector: 'jhi-user-mgmt',
  standalone: true,
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
  imports: [RouterModule, SharedModule, SortDirective, SortByDirective, ItemCountComponent],
})
export default class UserManagementComponent implements OnInit {
  currentAccount = inject(AccountService).trackCurrentAccount();
  users = signal<User[] | null>(null);
  isLoading = signal(false);
  totalItems = signal(0);
  page = 0;
  itemsPerPage = 20;
  sortState = sortStateSignal({});
  searchTerm = signal('');
  private searchTimeout: any;

  private readonly userService = inject(UserManagementService);
  private readonly sortService = inject(SortService);

  ngOnInit(): void {
    this.loadAll();
  }

  setActive(user: User, isActivated: boolean): void {
    this.userService.update({ ...user, activated: isActivated }).subscribe(() => this.loadAll());
  }

  trackIdentity(item: User): number;
  trackIdentity(index: number, item: User): number;
  trackIdentity(a: any, b?: any): number {
    const item: User = b ?? a;
    return item.id!;
  }

  loadAll(): void {
    this.isLoading.set(true);

    const params: any = {
      page: 0,
      size: 1000,
      sort: this.sortService.buildSortParam(this.sortState(), 'id'),
    };

    // Thêm tham số tìm kiếm nếu có
    if (this.searchTerm()) {
      params.login = this.searchTerm();
    }

    this.userService.query(params).subscribe({
      next: (res: HttpResponse<User[]>) => {
        this.isLoading.set(false);
        this.onSuccess(res.body, res.headers);
      },
      error: () => this.isLoading.set(false),
    });
  }

  // Public interaction methods (search + navigation)
  onSearchChange(): void {
    // Debounce search
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.searchTimeout = setTimeout(() => {
      this.loadAll();
    }, 300);
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);
    this.onSearchChange();
  }

  onClearSearch(): void {
    this.searchTerm.set('');
    this.loadAll();
  }

  transition(): void {
    this.loadAll();
  }

  private onSuccess(users: User[] | null, headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')));

    // Lọc người dùng dựa trên searchTerm nếu có
    if (users && this.searchTerm()) {
      const term = this.searchTerm().toLowerCase();
      const filtered = users.filter(
        user =>
          (user.login?.toLowerCase() ?? '').includes(term) ||
          (user.email?.toLowerCase() ?? '').includes(term) ||
          (user.firstName?.toLowerCase() ?? '').includes(term) ||
          (user.lastName?.toLowerCase() ?? '').includes(term) ||
          (user.phone?.toLowerCase() ?? '').includes(term),
      );
      this.users.set(filtered);
    } else {
      this.users.set(users);
    }
  }
}
