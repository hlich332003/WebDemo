import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IReview } from '../review.model';
import { ReviewService } from '../review.service';

@Component({
  standalone: true,
  templateUrl: './review-delete-dialog.component.html',
  imports: [SharedModule],
})
export class ReviewDeleteDialogComponent {
  review?: IReview;

  protected reviewService = inject(ReviewService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.reviewService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
