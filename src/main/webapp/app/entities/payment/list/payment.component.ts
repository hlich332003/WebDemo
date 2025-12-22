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
import { IPayment } from '../payment.model';
import { PaymentService, EntityArrayResponseType } from '../payment.service';
import { PaymentDeleteDialogComponent } from '../delete/payment-delete-dialog.component';
import { CastToDayjsPipe } from 'app/shared/date/cast-to-dayjs.pipe';

@Component({
  standalone: true,
  selector: 'jhi-payment',
  templateUrl: './payment.component.html',
  imports: [
    RouterModule,
    FormsModule,
    SharedModule,
    SortDirective,
    SortByDirective,
    FormatMediumDatetimePipe,
    CastToDayjsPipe,
  ],
})
export class PaymentComponent implements OnInit {
  payments?: IPayment[];
  isLoading = false;

  sortState: SortState = { predicate: 'id', order: 'asc' };

  protected paymentService = inject(PaymentService);
  protected activatedRoute = inject(ActivatedRoute);
  protected sortService = inject(SortService);
  protected modalService = inject(NgbModal);
  protected router = inject(Router);

  ngOnInit(): void {
    this.handleNavigation();
  }

  trackId = (_index: number, item: IPayment): number => item.id;

  delete(payment: IPayment): void {
    const modalRef = this.modalService.open(PaymentDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.payment = payment;
    modalRef.closed.subscribe((reason) => {
      if (reason === 'deleted') {
        this.load();
      }
    });
  }

  load(): void {
    this.queryBackend().subscribe({
      next: (res: HttpResponse<IPayment[]>) => {
        this.onSuccess(res.body, res.headers);
      },
    });
  }

  protected handleNavigation(): void {
    combineLatest(
      this.activatedRoute.queryParamMap,
      this.activatedRoute.data,
    ).subscribe(([params, data]) => {
      const sort = (params.get(SORT) ?? data['defaultSort']).split(',');
      this.sortState.predicate = sort[0];
      this.sortState.order = sort[1];
      this.load();
    });
  }

  protected onSuccess(data: IPayment[] | null, headers: HttpHeaders): void {
    this.payments = data ?? [];
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    this.isLoading = true;
    const queryObject: any = {
      sort: this.getSortQueryParam(),
    };
    return this.paymentService
      .query(queryObject)
      .pipe(tap(() => (this.isLoading = false)));
  }

  protected getSortQueryParam(
    predicate = this.sortState.predicate,
    order = this.sortState.order,
  ): string[] {
    const orderQueryParam = order;
    if (predicate === '') {
      return [];
    } else {
      return [predicate + ',' + orderQueryParam];
    }
  }
}
