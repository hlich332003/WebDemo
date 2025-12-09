import { Component, inject, signal } from '@angular/core';
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
  errorMessage = signal<string | null>(null);
  isDeleting = signal(false);

  protected productService = inject(ProductService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    console.log('Attempting to delete product with ID:', id);
    this.isDeleting.set(true);
    this.errorMessage.set(null);

    this.productService.delete(id).subscribe({
      next: (response) => {
        console.log('Delete successful:', response);
        this.isDeleting.set(false);
        this.activeModal.close(ITEM_DELETED_EVENT);
      },
      error: (error) => {
        console.error('Delete error - Full error object:', error);
        console.error('Error status:', error.status);
        console.error('Error message:', error.message);
        console.error('Error body:', error.error);

        this.isDeleting.set(false);

        let errorMsg = 'Không thể xóa sản phẩm này.';

        if (error.status === 401) {
          errorMsg = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
        } else if (error.status === 403) {
          errorMsg = 'Bạn không có quyền xóa sản phẩm này.';
        } else if (error.status === 400 && error.error?.message) {
          errorMsg = error.error.message;
        } else if (error.error?.detail) {
          errorMsg = error.error.detail;
        } else if (error.error?.title) {
          errorMsg = error.error.title;
        } else if (error.message) {
          errorMsg = error.message;
        }

        this.errorMessage.set(errorMsg);
      },
    });
  }
}
