import { Component, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';

import { IReview } from 'app/entities/review/review.model';
import { ReviewService } from 'app/entities/review/review.service';
import { ReviewDeleteDialogComponent } from 'app/entities/review/delete/review-delete-dialog.component';
import SharedModule from 'app/shared/shared.module';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { NotificationDatePipe } from '../../../shared/date/notification-date.pipe';

@Component({
  selector: 'jhi-review-management',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule, SharedModule, FormatMediumDatetimePipe, NotificationDatePipe],
  templateUrl: './review-management.component.html',
})
export class ReviewManagementComponent implements OnInit {
  reviews$: Observable<IReview[]>;

  constructor(
    private reviewService: ReviewService,
    private modalService: NgbModal,
  ) {
    this.reviews$ = new Observable<IReview[]>();
  }

  ngOnInit(): void {
    this.loadReviews();
  }

  loadReviews(): void {
    this.reviews$ = this.reviewService.query().pipe(map(res => res.body ?? []));
  }

  delete(review: IReview): void {
    const modalRef = this.modalService.open(ReviewDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.review = review;
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadReviews();
      }
    });
  }

  trackId(index: number, item: IReview): number {
    return item.id;
  }
}
