import { Component, AfterViewInit, ElementRef, ViewChild, inject } from '@angular/core';
import { FormGroup, FormControl, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import SharedModule from 'app/shared/shared.module';
import { LoginService } from 'app/login/login.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';

@Component({
  selector: 'jhi-login',
  standalone: true,
  imports: [SharedModule, FormsModule, ReactiveFormsModule, RouterModule, TranslateModule],
  templateUrl: './login.component.html',
})
export class LoginComponent implements AfterViewInit {
  @ViewChild('username', { static: false })
  username!: ElementRef;

  authenticationError = false;

  loginForm = new FormGroup({
    username: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    rememberMe: new FormControl(false, {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  private loginService = inject(LoginService);
  private router = inject(Router);
  private stateStorageService = inject(StateStorageService);

  ngAfterViewInit(): void {
    this.username.nativeElement.focus();
  }

  login(): void {
    this.loginService.login(this.loginForm.getRawValue()).subscribe({
      next: account => {
        this.authenticationError = false;
        if (account?.authorities?.includes('ROLE_ADMIN')) {
          this.router.navigate(['/admin']);
        } else {
          const previousUrl = this.stateStorageService.getUrl();
          if (previousUrl) {
            this.stateStorageService.clearUrl();
            this.router.navigateByUrl(previousUrl);
          } else {
            this.router.navigate(['']);
          }
        }
      },
      error: () => (this.authenticationError = true),
    });
  }
}
