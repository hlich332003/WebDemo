import { AfterViewInit, Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { LoginService } from 'app/login/login.service';
import { AccountService } from 'app/core/auth/account.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';

@Component({
  selector: 'jhi-login',
  standalone: true,
  templateUrl: './login.component.html',

  imports: [SharedModule, FormsModule, ReactiveFormsModule, RouterModule],
})
export class LoginComponent implements OnInit, AfterViewInit {
  username = viewChild.required<ElementRef>('username');

  authenticationError = signal(false);

  loginForm = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    rememberMe: new FormControl(false, { nonNullable: true }),
  });

  private readonly accountService = inject(AccountService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly stateStorageService = inject(StateStorageService);

  ngOnInit(): void {
    // Tạm tắt auto-redirect để test login
    // if (this.accountService.isAuthenticated()) {
    //   this.navigateBasedOnRole();
    // }
  }

  ngAfterViewInit(): void {
    try {
      this.username().nativeElement.focus();
    } catch (error) {
      console.error('Failed to focus username field:', error);
    }
  }

  login(event: Event): void {
    event.preventDefault();
    console.log('Login method called');
    console.log('Form valid:', this.loginForm.valid);
    console.log('Form value:', this.loginForm.getRawValue());

    if (this.loginForm.invalid) {
      console.log('Form invalid, stopping');
      return;
    }

    this.stateStorageService.clearUrl();
    const credentials = this.loginForm.getRawValue();
    console.log('Calling loginService.login with:', credentials);

    this.loginService.login(credentials).subscribe({
      next: account => {
        console.log('Login success:', account);
        this.authenticationError.set(false);
        this.navigateBasedOnRole(account);
      },
      error: error => {
        console.error('Login failed:', error);
        this.authenticationError.set(true);
      },
    });
  }

  private navigateBasedOnRole(account?: any): void {
    const userAccount = account ?? this.accountService.trackCurrentAccount()();
    if (userAccount?.authorities?.includes('ROLE_ADMIN')) {
      this.router.navigate(['/admin']);
    } else {
      this.router.navigate(['']);
    }
  }
}
