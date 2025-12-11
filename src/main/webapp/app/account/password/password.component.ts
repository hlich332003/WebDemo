import { Component, inject, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import SharedModule from 'app/shared/shared.module';
import { AccountService } from 'app/core/auth/account.service';
import { PasswordService } from './password.service';

@Component({
  selector: 'jhi-password',
  standalone: true,
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
  templateUrl: './password.component.html',
})
export default class PasswordComponent {
  doNotMatch = signal(false);
  error = signal(false);
  success = signal(false);
  account = inject(AccountService).trackCurrentAccount();

  passwordForm = new FormGroup({
    currentPassword: new FormControl('', {
      nonNullable: true,
      validators: Validators.required,
    }),
    newPassword: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(4),
        Validators.maxLength(50),
      ],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(4),
        Validators.maxLength(50),
      ],
    }),
  });

  private readonly passwordService = inject(PasswordService);

  changePassword(): void {
    this.error.set(false);
    this.success.set(false);
    this.doNotMatch.set(false);

    const { newPassword, confirmPassword, currentPassword } =
      this.passwordForm.getRawValue();
    if (newPassword !== confirmPassword) {
      this.doNotMatch.set(true);
    } else {
      this.passwordService.save(newPassword, currentPassword).subscribe({
        next: () => this.success.set(true),
        error: () => this.error.set(true),
      });
    }
  }
}
