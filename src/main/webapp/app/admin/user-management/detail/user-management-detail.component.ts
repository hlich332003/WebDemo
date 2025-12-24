import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';
import SharedModule from 'app/shared/shared.module';

import { User } from '../user-management.model';

@Component({
  selector: 'jhi-user-mgmt-detail',
  standalone: true,
  templateUrl: './user-management-detail.component.html',
  imports: [RouterModule, SharedModule],
})
export default class UserManagementDetailComponent {
  user = input<User | null>(null);
}
