import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import SharedModule from 'app/shared/shared.module';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';

@Component({
  selector: 'jhi-settings',
  standalone: true,
  imports: [SharedModule, FormsModule, ReactiveFormsModule, TranslateModule],
  templateUrl: './settings.component.html',
})
export default class SettingsComponent implements OnInit {
  success = signal(false);
  currentAccount: Account | null = null;

  settingsForm = new FormGroup({
    firstName: new FormControl('', {
      validators: [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(50),
      ],
    }),
    lastName: new FormControl('', {
      validators: [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(50),
      ],
    }),
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
  });

  private readonly accountService = inject(AccountService);

  ngOnInit(): void {
    this.accountService.identity().subscribe((account) => {
      if (account) {
        this.settingsForm.patchValue({
          firstName: account.firstName,
          lastName: account.lastName,
          email: account.email,
          phone: account.phone,
        });
        this.currentAccount = account;
      }
    });
  }

  save(): void {
    this.success.set(false);

    const formValues = this.settingsForm.getRawValue();
    const account: Account = {
      ...this.currentAccount!,
      firstName: formValues.firstName,
      lastName: formValues.lastName,
      email: formValues.email, // email is non-nullable in the form
      phone: formValues.phone,
    };

    this.accountService.save(account).subscribe(() => {
      this.success.set(true);
      this.accountService.authenticate(account);
    });
  }
}
