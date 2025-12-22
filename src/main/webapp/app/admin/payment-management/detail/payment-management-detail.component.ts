import { Component, Input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

import { IPayment } from 'app/entities/payment/payment.model';
import SharedModule from 'app/shared/shared.module';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { NotificationDatePipe } from '../../../shared/date/notification-date.pipe';

@Component({
  selector: 'jhi-payment-management-detail',
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
    SharedModule,
    FormatMediumDatetimePipe,
    NotificationDatePipe,
  ],
  templateUrl: './payment-management-detail.component.html',
})
export class PaymentManagementDetailComponent {
  @Input() payment: IPayment | null = null;

  previousState(): void {
    window.history.back();
  }
}
