import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { NotificationService } from 'app/shared/notification/notification.service';
import { LoginService } from 'app/login/login.service'; // Import LoginService

const initialAccount: Account = {
  firstName: null,
  lastName: null,
  email: '',
  langKey: '',
  activated: false,
  authorities: [],
  login: '',
  imageUrl: null,
  phone: null,
} as Account;

@Component({
  selector: 'jhi-settings',
  standalone: true,
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
  templateUrl: './settings.component.html',
})
export default class SettingsComponent implements OnInit {
  success = signal(false);
  currentAccount: Account | null = null;

  settingsForm = new FormGroup({
    firstName: new FormControl(initialAccount.firstName, {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(50),
      ],
    }),
    lastName: new FormControl(initialAccount.lastName, {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(50),
      ],
    }),
    email: new FormControl(initialAccount.email, {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(5),
        Validators.maxLength(254),
        Validators.email,
      ],
    }),
    phone: new FormControl(initialAccount.phone, {
      nonNullable: true,
      validators: [Validators.maxLength(20)],
    }),
    langKey: new FormControl(initialAccount.langKey, {
      nonNullable: true,
    }),
    activated: new FormControl(initialAccount.activated, {
      nonNullable: true,
    }),
    authorities: new FormControl(initialAccount.authorities, {
      nonNullable: true,
    }),
    imageUrl: new FormControl(initialAccount.imageUrl, {
      nonNullable: true,
    }),
    login: new FormControl(initialAccount.login, {
      nonNullable: true,
    }),
  });

  private readonly accountService = inject(AccountService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly loginService = inject(LoginService); // Inject LoginService

  ngOnInit(): void {
    this.accountService.identity().subscribe((account) => {
      if (account) {
        this.currentAccount = account;
        this.settingsForm.patchValue(account);
      }
    });
  }

  save(): void {
    this.success.set(false);

    const account = this.settingsForm.getRawValue();
    const emailChanged = this.currentAccount?.email !== account.email;

    this.accountService.save(account).subscribe({
      next: () => {
        this.success.set(true);
        if (emailChanged) {
          this.notify.info(
            'Email của bạn đã được thay đổi. Vui lòng đăng nhập lại bằng email mới.',
          );
          this.loginService.logout(); // Sử dụng loginService.logout()
          this.router.navigate(['/login']);
        } else {
          this.accountService.authenticate(account);
          this.notify.success('Cài đặt đã được lưu thành công!');
        }
      },
      error: () => {
        this.notify.error('Lưu cài đặt thất bại!');
      },
    });
  }
}
