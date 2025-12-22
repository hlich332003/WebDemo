import { Component, Input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

import { IReview } from 'app/entities/review/review.model';
import SharedModule from 'app/shared/shared.module';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { NotificationDatePipe } from '../../../shared/date/notification-date.pipe';

@Component({
  selector: 'jhi-review-management-detail',
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
    SharedModule,
    FormatMediumDatetimePipe,
    NotificationDatePipe,
  ],
  templateUrl: './review-management-detail.component.html',
})
export class ReviewManagementDetailComponent {
  @Input() review: IReview | null = null;

  previousState(): void {
    window.history.back();
  }
}
