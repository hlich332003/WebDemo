import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IProduct } from 'app/entities/product/product.model'; // Import từ file model mới
import { ProductService } from 'app/entities/product/product.service';

@Component({
  standalone: true,
  templateUrl: './product-delete-dialog.component.html',
  imports: [SharedModule],
})
export default class ProductDeleteDialogComponent {
  product?: IProduct;

  protected productService = inject(ProductService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.productService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
