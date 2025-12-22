import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IPayment } from '../payment.model';
import { PaymentService } from '../payment.service';

@Component({
  standalone: true,
  templateUrl: './payment-delete-dialog.component.html',
  imports: [SharedModule],
})
export class PaymentDeleteDialogComponent {
  payment?: IPayment;

  protected paymentService = inject(PaymentService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.paymentService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
