import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { IUser } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';

const userTemplate: IUser = {
  imageUrl: null,
} as IUser;

const newUser: IUser = {
  activated: true,
  imageUrl: null,
} as IUser;

@Component({
  selector: 'jhi-user-mgmt-update',
  standalone: true,
  templateUrl: './user-management-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export default class UserManagementUpdateComponent implements OnInit {
  authorities = signal<string[]>([]);
  isSaving = signal(false);

  editForm = new FormGroup({
    id: new FormControl(userTemplate.id),
    firstName: new FormControl(userTemplate.firstName, {
      validators: [Validators.maxLength(50)],
    }),
    lastName: new FormControl(userTemplate.lastName, {
      validators: [Validators.maxLength(50)],
    }),
    email: new FormControl(userTemplate.email, {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
    activated: new FormControl(userTemplate.activated, { nonNullable: true }),
    authority: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    phone: new FormControl(userTemplate.phone, {
      validators: [Validators.maxLength(20)],
    }),
    imageUrl: new FormControl(userTemplate.imageUrl, {
      validators: [Validators.maxLength(256)],
    }),
  });

  private readonly userService = inject(UserManagementService);
  private readonly route = inject(ActivatedRoute);

  ngOnInit(): void {
    this.route.data.subscribe(({ user }) => {
      if (user) {
        this.updateForm(user);
      } else {
        this.editForm.patchValue({ ...newUser, authority: 'ROLE_USER' });
      }
    });
    this.userService.authorities().subscribe(authorities => this.authorities.set(authorities));
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const user = this.createUserFromForm();
    if (user.id !== null) {
      this.userService.update(user).subscribe({
        next: () => this.onSaveSuccess(),
        error: () => this.onSaveError(),
      });
    } else {
      this.userService.create(user).subscribe({
        next: () => this.onSaveSuccess(),
        error: () => this.onSaveError(),
      });
    }
  }

  private onSaveSuccess(): void {
    this.isSaving.set(false);
    this.previousState();
  }

  private onSaveError(): void {
    this.isSaving.set(false);
  }

  private updateForm(user: IUser): void {
    // Get the first authority or empty string if no authorities
    const userAuthority = user.authorities && user.authorities.length > 0 ? user.authorities[0] : '';

    this.editForm.patchValue({
      id: user.id,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      activated: user.activated,
      phone: user.phone,
      imageUrl: user.imageUrl,
      authority: userAuthority,
    });

    console.log('DEBUG: User authorities:', user.authorities);
    console.log('DEBUG: Set authority to:', userAuthority);
  }

  private createUserFromForm(): IUser {
    const { authority, ...formValue } = this.editForm.getRawValue();
    return {
      ...formValue,
      id: formValue.id,
      authorities: authority ? [authority] : [],
    };
  }
}
