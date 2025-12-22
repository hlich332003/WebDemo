import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { IPayment } from '../payment.model';
import { CastToDayjsPipe } from 'app/shared/date/cast-to-dayjs.pipe';

@Component({
  standalone: true,
  selector: 'jhi-payment-detail',
  templateUrl: './payment-detail.component.html',
  imports: [
    SharedModule,
    RouterModule,
    FormatMediumDatetimePipe,
    CastToDayjsPipe,
  ],
})
export class PaymentDetailComponent {
  payment = input<IPayment | null>(null);

  previousState(): void {
    window.history.back();
  }
}
