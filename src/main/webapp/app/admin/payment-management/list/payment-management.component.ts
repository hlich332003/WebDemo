import { Component, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';

import { IPayment } from 'app/entities/payment/payment.model';
import { PaymentService } from 'app/entities/payment/payment.service';
import { PaymentDeleteDialogComponent } from 'app/entities/payment/delete/payment-delete-dialog.component';
import SharedModule from 'app/shared/shared.module';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { NotificationDatePipe } from '../../../shared/date/notification-date.pipe';

@Component({
  selector: 'jhi-payment-management',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule, SharedModule, FormatMediumDatetimePipe, NotificationDatePipe],
  templateUrl: './payment-management.component.html',
})
export class PaymentManagementComponent implements OnInit {
  payments$: Observable<IPayment[]>;

  constructor(
    private paymentService: PaymentService,
    private modalService: NgbModal,
  ) {
    this.payments$ = new Observable<IPayment[]>();
  }

  ngOnInit(): void {
    this.loadPayments();
  }

  loadPayments(): void {
    this.payments$ = this.paymentService.query().pipe(map(res => res.body ?? []));
  }

  delete(payment: IPayment): void {
    const modalRef = this.modalService.open(PaymentDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.payment = payment;
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadPayments();
      }
    });
  }

  trackId(index: number, item: IPayment): number {
    return item.id;
  }
}
