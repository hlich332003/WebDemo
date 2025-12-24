import { Component, OnInit, inject, signal, ViewChild, ElementRef } from '@angular/core';
import { RouterModule } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

import SharedModule from 'app/shared/shared.module';
import { SortByDirective, SortDirective, SortService, sortStateSignal } from 'app/shared/sort';
import { AccountService } from 'app/core/auth/account.service';
import { UserManagementService } from '../service/user-management.service';
import { User } from '../user-management.model';
import ItemCountComponent from 'app/shared/pagination/item-count.component';
import { UserImportService } from '../service/user-import.service';
import { NotificationService } from 'app/shared/notification/notification.service';

@Component({
  selector: 'jhi-user-mgmt',
  standalone: true,
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
  imports: [RouterModule, SharedModule, SortDirective, SortByDirective, ItemCountComponent],
})
export default class UserManagementComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef;

  currentAccount = inject(AccountService).trackCurrentAccount();
  users = signal<User[] | null>(null);
  isLoading = signal(false);
  totalItems = signal(0);
  page = 1; // Start with page 1
  itemsPerPage = 20;
  sortState = sortStateSignal({ predicate: 'id', order: 'asc' });
  searchTerm = signal('');
  private searchTimeout: any;

  private readonly userService = inject(UserManagementService);
  private readonly userImportService = inject(UserImportService);
  private readonly sortService = inject(SortService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    this.loadAll();
  }

  setActive(user: User, isActivated: boolean): void {
    this.userService.update({ ...user, activated: isActivated }).subscribe(() => this.loadAll());
  }

  loadAll(): void {
    this.isLoading.set(true);
    this.userService
      .query({
        page: this.page - 1,
        size: this.itemsPerPage,
        sort: this.sortService.buildSortParam(this.sortState()),
      })
      .subscribe({
        next: (res: HttpResponse<User[]>) => {
          this.isLoading.set(false);
          this.onSuccess(res.body, res.headers);
        },
        error: () => this.isLoading.set(false),
      });
  }

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.isLoading.set(true);
      this.userImportService.importUsers(file).subscribe({
        next: () => {
          this.isLoading.set(false);
          this.notificationService.success('Tải lên người dùng thành công!');
          this.loadAll();
        },
        error: err => {
          this.isLoading.set(false);
          this.notificationService.error('Lỗi khi tải lên người dùng: ' + err.message);
        },
      });
    }
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);
    // No need to call loadAll, filtering is done on the client side
  }

  onClearSearch(): void {
    this.searchTerm.set('');
  }

  transition(): void {
    this.loadAll();
  }

  private onSuccess(users: User[] | null, headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')));
    // The filtering logic will be handled by a computed signal or a pipe in the template
    this.users.set(users);
  }

  // We need a computed signal to handle filtering
  filteredUsers = () => {
    const usersList = this.users();
    const term = this.searchTerm().toLowerCase();
    if (!usersList || !term) {
      return usersList;
    }
    return usersList.filter(
      user =>
        (user.login?.toLowerCase() ?? '').includes(term) ||
        (user.email?.toLowerCase() ?? '').includes(term) ||
        (user.firstName?.toLowerCase() ?? '').includes(term) ||
        (user.lastName?.toLowerCase() ?? '').includes(term) ||
        (user.phone?.toLowerCase() ?? '').includes(term),
    );
  };
}
