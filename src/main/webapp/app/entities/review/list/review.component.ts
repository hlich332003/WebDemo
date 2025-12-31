import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { combineLatest, Observable, tap } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpResponse, HttpHeaders } from '@angular/common/http';

import SharedModule from 'app/shared/shared.module';
import { SortDirective, SortByDirective, SortState } from 'app/shared/sort';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { FormsModule } from '@angular/forms';
import { ASC, DESC, SORT } from 'app/config/navigation.constants';
import { SortService } from 'app/shared/sort/sort.service';
import { IReview } from '../review.model';
import { ReviewService, EntityArrayResponseType } from '../review.service';
import { ReviewDeleteDialogComponent } from '../delete/review-delete-dialog.component';
import { CastToDayjsPipe } from 'app/shared/date/cast-to-dayjs.pipe';

@Component({
  standalone: true,
  selector: 'jhi-review',
  templateUrl: './review.component.html',
  imports: [RouterModule, FormsModule, SharedModule, SortDirective, SortByDirective, FormatMediumDatetimePipe, CastToDayjsPipe],
})
export class ReviewComponent implements OnInit {
  reviews?: IReview[];
  isLoading = false;

  sortState: SortState = { predicate: 'id', order: 'asc' };

  protected reviewService = inject(ReviewService);
  protected activatedRoute = inject(ActivatedRoute);
  protected sortService = inject(SortService);
  protected modalService = inject(NgbModal);
  protected router = inject(Router);

  ngOnInit(): void {
    this.handleNavigation();
  }

  trackId = (_index: number, item: IReview): number => item.id;

  delete(review: IReview): void {
    const modalRef = this.modalService.open(ReviewDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.review = review;
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.load();
      }
    });
  }

  load(): void {
    this.queryBackend().subscribe({
      next: (res: HttpResponse<IReview[]>) => {
        this.onSuccess(res.body, res.headers);
      },
    });
  }

  protected handleNavigation(): void {
    combineLatest(this.activatedRoute.queryParamMap, this.activatedRoute.data).subscribe(([params, data]) => {
      const sort = (params.get(SORT) ?? data['defaultSort']).split(',');
      this.sortState.predicate = sort[0];
      this.sortState.order = sort[1];
      this.load();
    });
  }

  protected onSuccess(data: IReview[] | null, headers: HttpHeaders): void {
    this.reviews = data ?? [];
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    this.isLoading = true;
    const queryObject: any = {
      sort: this.getSortQueryParam(),
    };
    return this.reviewService.query(queryObject).pipe(tap(() => (this.isLoading = false)));
  }

  protected getSortQueryParam(predicate = this.sortState.predicate, order = this.sortState.order): string[] {
    const orderQueryParam = order;
    if (predicate === '') {
      return [];
    } else {
      return [predicate + ',' + orderQueryParam];
    }
  }
}
