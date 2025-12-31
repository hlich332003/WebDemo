import { AfterViewInit, Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import SharedModule from 'app/shared/shared.module';
import { RegisterService } from './register.service';
import { LoginService } from 'app/login/login.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'jhi-register',
  standalone: true,
  imports: [SharedModule, FormsModule, ReactiveFormsModule, RouterModule, TranslateModule],
  templateUrl: './register.component.html',
})
export default class RegisterComponent implements AfterViewInit {
  @ViewChild('email', { static: false })
  email?: ElementRef;

  doNotMatch = signal(false);
  error = signal(false);
  errorEmailExists = signal(false);
  errorPhoneExists = signal(false);
  errorInvalidPassword = signal(false);
  errorMessage = signal('');
  success = signal(false);

  registerForm = new FormGroup({
    firstName: new FormControl(''),
    lastName: new FormControl(''),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
    phone: new FormControl(''),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)],
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
    this.errorPhoneExists.set(false);
    this.errorInvalidPassword.set(false);
    this.errorMessage.set('');

    const password = this.registerForm.get(['password'])!.value;
    if (password !== this.registerForm.get(['confirmPassword'])!.value) {
      this.doNotMatch.set(true);
    } else {
      const { firstName, lastName, email, phone, password: userPassword } = this.registerForm.getRawValue();
      this.registerService
        .save({
          firstName,
          lastName,
          email,
          phone,
          password: userPassword,
          langKey: 'vi',
        })
        .subscribe({
          next: () => {
            this.success.set(true);
            this.notify.success('Đăng ký tài khoản thành công! Đang đăng nhập...');
            this.loginService.login({ username: email, password: userPassword, rememberMe: false }).subscribe({
              next: () => {
                this.router.navigate(['/']);
                this.notify.success('Đăng nhập thành công!');
              },
              error: () => {
                this.notify.error('Đăng nhập tự động thất bại. Vui lòng đăng nhập thủ công.');
                this.router.navigate(['/login']);
              },
            });
          },
          error: response => this.processError(response),
        });
    }
  }

  private processError(response: HttpErrorResponse): void {
    console.error('Registration error:', response);
    console.error('Response status:', response.status);
    console.error('Response error:', response.error);
    console.error('Response headers:', response.headers);

    // Reset all error states
    this.error.set(false);
    this.errorEmailExists.set(false);
    this.errorPhoneExists.set(false);
    this.errorInvalidPassword.set(false);
    this.errorMessage.set('');

    if (response.status === 400) {
      // JHipster puts errorKey in HTTP headers: X-webDemoApp-error
      const errorKey = response.headers.get('X-webDemoApp-error') ?? response.error?.errorKey;
      const title = response.error?.title ?? response.error?.message;
      const detail = response.error?.detail;

      console.log('Error key from header:', response.headers.get('X-webDemoApp-error'));
      console.log('Error key from body:', response.error?.errorKey);
      console.log('Final error key:', errorKey);
      console.log('Error title:', title);
      console.log('Error detail:', detail);

      // Handle specific error types by errorKey
      if (errorKey === 'emailexists') {
        this.errorEmailExists.set(true);
        this.errorMessage.set(title ?? 'Email này đã được sử dụng. Vui lòng chọn email khác.');
        this.notify.error('Email đã tồn tại!');
      } else if (errorKey === 'phoneexists') {
        this.errorPhoneExists.set(true);
        this.errorMessage.set(title ?? 'Số điện thoại này đã được sử dụng. Vui lòng chọn số khác.');
        this.notify.error('Số điện thoại đã tồn tại!');
      } else if (errorKey === 'incorrectpassword') {
        this.errorInvalidPassword.set(true);
        this.errorMessage.set(title ?? 'Mật khẩu không hợp lệ. Mật khẩu phải có ít nhất 4 ký tự.');
        this.notify.error('Mật khẩu không hợp lệ!');
      } else {
        // Generic 400 error
        this.error.set(true);
        const message = detail ?? title ?? 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin.';
        this.errorMessage.set(message);
        this.notify.error(message);
      }
    } else if (response.status === 500) {
      this.error.set(true);
      this.errorMessage.set('Lỗi server. Vui lòng thử lại sau.');
      this.notify.error('Lỗi server!');
    } else if (response.status === 0) {
      this.error.set(true);
      this.errorMessage.set('Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.');
      this.notify.error('Không thể kết nối!');
    } else {
      this.error.set(true);
      const message = response.error?.message ?? response.message ?? 'Đăng ký thất bại. Vui lòng thử lại.';
      this.errorMessage.set(message);
      this.notify.error('Đăng ký thất bại!');
    }
  }
}
