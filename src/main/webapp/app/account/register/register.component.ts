import {
  AfterViewInit,
  Component,
  ElementRef,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { RegisterService } from './register.service';
import { LoginService } from 'app/login/login.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'jhi-register',
  standalone: true,
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
})
export default class RegisterComponent implements AfterViewInit {
  @ViewChild('email', { static: false })
  email?: ElementRef;

  doNotMatch = signal(false);
  error = signal(false);
  errorEmailExists = signal(false);
  success = signal(false);

  registerForm = new FormGroup({
    firstName: new FormControl(''),
    lastName: new FormControl(''),
    email: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(5),
        Validators.maxLength(254),
        Validators.email,
      ],
    }),
    phone: new FormControl(''),
    password: new FormControl('', {
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

  private registerService = inject(RegisterService);
  private loginService = inject(LoginService);
  private router = inject(Router);
  private notify = inject(NotificationService);

  ngAfterViewInit(): void {
    this.email?.nativeElement.focus();
  }

  register(): void {
    this.doNotMatch.set(false);
    this.error.set(false);
    this.errorEmailExists.set(false);

    const password = this.registerForm.get(['password'])!.value;
    if (password !== this.registerForm.get(['confirmPassword'])!.value) {
      this.doNotMatch.set(true);
    } else {
      const { firstName, lastName, email, phone, password } =
        this.registerForm.getRawValue();
      this.registerService
        .save({
          firstName,
          lastName,
          email,
          phone,
          password,
          langKey: 'vi',
        })
        .subscribe({
          next: () => {
            this.success.set(true);
            this.notify.success(
              'Đăng ký tài khoản thành công! Đang đăng nhập...',
            );
            this.loginService
              .login({ username: email, password: password, rememberMe: false })
              .subscribe({
                next: () => {
                  this.router.navigate(['/']);
                  this.notify.success('Đăng nhập thành công!');
                },
                error: () => {
                  this.notify.error(
                    'Đăng nhập tự động thất bại. Vui lòng đăng nhập thủ công.',
                  );
                  this.router.navigate(['/login']);
                },
              });
          },
          error: (response) => this.processError(response),
        });
    }
  }

  private processError(response: HttpErrorResponse): void {
    if (
      response.status === 400 &&
      response.error?.type === 'EMAIL_ALREADY_USED'
    ) {
      this.errorEmailExists.set(true);
    } else {
      this.error.set(true);
    }
  }
}
