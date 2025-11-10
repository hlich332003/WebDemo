import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http'; // Import HttpErrorResponse
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';

import { IProduct, NewProduct } from 'app/entities/product/product.model';
import { ProductService } from 'app/entities/product/product.service';
import { NotificationService } from 'app/shared/notification/notification.service';

type ProductFormGroupContent = {
  id: FormControl<IProduct['id'] | null>;
  name: FormControl<IProduct['name']>;
  description: FormControl<IProduct['description']>;
  price: FormControl<IProduct['price']>;
  quantity: FormControl<IProduct['quantity']>;
  imageUrl: FormControl<IProduct['imageUrl']>;
};

type ProductFormGroup = FormGroup<ProductFormGroupContent>;

@Component({
  selector: 'jhi-product-update',
  standalone: true,
  imports: [SharedModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './product-update.component.html',
})
export default class ProductUpdateComponent implements OnInit {
  isSaving = false;
  product: IProduct | null = null;
  isEditing = signal(false);

  protected productService = inject(ProductService);
  protected activatedRoute = inject(ActivatedRoute);
  private router = inject(Router);
  private notify = inject(NotificationService);

  editForm: ProductFormGroup = new FormGroup<ProductFormGroupContent>({
    id: new FormControl(null as IProduct['id'] | null, { nonNullable: true }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(3), Validators.maxLength(100)] }),
    description: new FormControl(null as IProduct['description'], { validators: [Validators.maxLength(500)] }),
    price: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    quantity: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    imageUrl: new FormControl(null as IProduct['imageUrl']),
  });

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ product, isEditing }) => {
      this.product = product;
      this.isEditing.set(isEditing ?? product === null);

      if (product) {
        this.updateForm(product);
      }

      if (!this.isEditing()) {
        this.editForm.disable();
      }
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const product = this.createFromForm();
    if (product.id !== null) {
      this.subscribeToSaveResponse(this.productService.update(product));
    } else {
      this.subscribeToSaveResponse(this.productService.create(product));
    }
  }

  switchToEditMode(): void {
    if (this.product?.id) {
      this.router.navigate(['/admin/product-management', this.product.id, 'edit']);
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IProduct>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: (error: HttpErrorResponse) => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(): void {
    this.notify.success('Lưu sản phẩm thành công!');
    this.previousState();
  }

  protected onSaveError(error: HttpErrorResponse): void {
    const errorMessage = error.error?.detail || error.message || 'Lưu sản phẩm thất bại.';
    this.notify.error(errorMessage);
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(product: IProduct): void {
    this.editForm.patchValue({
      id: product.id,
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity,
      imageUrl: product.imageUrl,
    });
  }

  protected createFromForm(): IProduct | NewProduct {
    return {
      ...this.editForm.getRawValue(),
      id: this.editForm.get('id')!.value,
    };
  }
}
