import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { IUser } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';

const userTemplate: IUser = {
  imageUrl: null, // Thêm lại imageUrl
} as IUser;

const newUser: IUser = {
  activated: true,
  imageUrl: null, // Thêm lại imageUrl
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
    login: new FormControl(userTemplate.login, {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(50),
        Validators.pattern('^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$'),
      ],
    }),
    firstName: new FormControl(userTemplate.firstName, {
      validators: [Validators.maxLength(50)],
    }),
    lastName: new FormControl(userTemplate.lastName, {
      validators: [Validators.maxLength(50)],
    }),
    email: new FormControl(userTemplate.email, {
      nonNullable: true,
      validators: [
        Validators.minLength(5),
        Validators.maxLength(254),
        Validators.email,
      ],
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
    }), // Thêm lại imageUrl
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
    this.editForm.patchValue({
      id: user.id,
      login: user.login,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      activated: user.activated,
      phone: user.phone,
      imageUrl: user.imageUrl, // Thêm lại imageUrl
      authority: user.authorities?.[0] ?? 'ROLE_USER',
    });
  }

  private createUserFromForm(): IUser {
    const { authority, ...formValue } = this.editForm.getRawValue();
    return {
      ...formValue,
      id: formValue.id,
      authorities: [authority],
    };
  }
}
