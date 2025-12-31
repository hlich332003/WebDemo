import { Component, OnInit, inject, signal, ViewChild, ElementRef } from '@angular/core';
import { RouterModule } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import SharedModule from 'app/shared/shared.module';
import { SortByDirective, SortDirective, SortService, sortStateSignal } from 'app/shared/sort';
import { AccountService } from 'app/core/auth/account.service';
import { UserManagementService } from '../service/user-management.service';
import { User } from '../user-management.model';
import ItemCountComponent from 'app/shared/pagination/item-count.component';
import { UserImportService } from '../service/user-import.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import UserManagementDeleteDialogComponent from '../delete/user-management-delete-dialog.component';

@Component({
  selector: 'jhi-user-mgmt',
  standalone: true,
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
  imports: [RouterModule, SharedModule, SortDirective, SortByDirective, ItemCountComponent, FontAwesomeModule],
})
export default class UserManagementComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef;

  currentAccount = inject(AccountService).trackCurrentAccount();
  users = signal<User[] | null>(null);
  isLoading = signal(false);
  totalItems = signal(0);
  page = 1;
  itemsPerPage = 20;
  sortState = sortStateSignal({ predicate: 'id', order: 'asc' });
  searchTerm = signal('');
  private searchTimeout: any;

  private readonly userService = inject(UserManagementService);
  private readonly userImportService = inject(UserImportService);
  private readonly sortService = inject(SortService);
  private readonly notificationService = inject(NotificationService);
  private readonly modalService = inject(NgbModal);

  ngOnInit(): void {
    this.loadAll();
  }

  setActive(user: User, isActivated: boolean): void {
    this.userService.update({ ...user, activated: isActivated }).subscribe(() => this.loadAll());
  }

  loadAll(): void {
    this.isLoading.set(true);

    const queryParams: any = {
      page: this.page - 1,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState()),
    };

    if (this.searchTerm()) {
      queryParams['login.contains'] = this.searchTerm();
    }

    this.userService.query(queryParams).subscribe({
      next: (res: HttpResponse<User[]>) => {
        this.isLoading.set(false);
        this.onSuccess(res.body, res.headers);
      },
      error: () => this.isLoading.set(false),
    });
  }

  deleteUser(user: User): void {
    const modalRef = this.modalService.open(UserManagementDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.user = user;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadAll();
      }
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
        error: (err: { message: string }) => {
          this.isLoading.set(false);
          this.notificationService.error(`Lỗi khi tải lên người dùng: ${err.message}`);
        },
      });
    }
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);

    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.searchTimeout = setTimeout(() => {
      this.page = 1;
      this.loadAll();
    }, 500);
  }

  onClearSearch(): void {
    this.searchTerm.set('');
    this.page = 1;
    this.loadAll();
  }

  exportUsers(): void {
    const users = this.users();
    if (!users || users.length === 0) {
      this.notificationService.error('Không có dữ liệu để xuất');
      return;
    }

    this.isLoading.set(true);
    this.userImportService.exportUsers(users).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.notificationService.success('Xuất dữ liệu người dùng thành công!');
      },
      error: (err: { message: string }) => {
        this.isLoading.set(false);
        this.notificationService.error(`Lỗi khi xuất dữ liệu: ${err.message}`);
      },
    });
  }

  transition(): void {
    this.loadAll();
  }

  private onSuccess(users: User[] | null, headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')));
    this.users.set(users);
  }
}
