import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';

import { IProduct, NewProduct } from 'app/entities/product/product.model';
import { ProductService } from 'app/entities/product/product.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { CategoryService } from 'app/entities/category/category.service'; // Import CategoryService
import { ICategory } from 'app/entities/product/category.model'; // Import ICategory từ product folder

type ProductFormGroupContent = {
  id: FormControl<IProduct['id'] | null>;
  name: FormControl<IProduct['name']>;
  description: FormControl<IProduct['description']>;
  price: FormControl<IProduct['price']>;
  quantity: FormControl<IProduct['quantity']>;
  imageUrl: FormControl<IProduct['imageUrl']>;
  salesCount: FormControl<IProduct['salesCount']>;
  category: FormControl<IProduct['category']>; // Thêm FormControl cho category
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
  categories: ICategory[] = []; // Danh sách danh mục

  // Image upload properties
  imageUploadMethod: 'url' | 'upload' = 'url';
  isUploading = false;
  uploadError: string | null = null;
  selectedFile: File | null = null;

  editForm: ProductFormGroup = new FormGroup<ProductFormGroupContent>({
    id: new FormControl(null as IProduct['id'] | null, { nonNullable: true }),
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3), Validators.maxLength(100)],
    }),
    description: new FormControl(null as IProduct['description'], {
      validators: [Validators.maxLength(500)],
    }),
    price: new FormControl(0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0)],
    }),
    quantity: new FormControl(0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0)],
    }),
    imageUrl: new FormControl(null as IProduct['imageUrl']),
    salesCount: new FormControl(0, { nonNullable: true }),
    category: new FormControl(null as IProduct['category'], {
      validators: [Validators.required],
    }), // Khởi tạo FormControl cho category
  });

  protected productService = inject(ProductService);
  protected activatedRoute = inject(ActivatedRoute);
  private router = inject(Router);
  private notify = inject(NotificationService);
  private categoryService = inject(CategoryService); // Inject CategoryService

  ngOnInit(): void {
    // Load categories first, then handle product data
    this.categoryService.query().subscribe(res => {
      this.categories = (res.body ?? []) as ICategory[];
      console.warn('DEBUG: Categories loaded from server:', this.categories);

      // After categories are loaded, handle product data
      this.activatedRoute.data.subscribe(({ product, isEditing }) => {
        this.product = product;
        this.isEditing.set(isEditing ?? product === null);

        if (product) {
          console.warn('DEBUG: Product loaded from route:', product);
          // Sử dụng setTimeout để đảm bảo view đã được render với categories
          setTimeout(() => {
            this.updateForm(product);
          }, 0);
        }

        if (!this.isEditing()) {
          this.editForm.disable();
        }
      });
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

  compareCategory(o1: ICategory | null, o2: ICategory | null): boolean {
    // Compare by numeric id to tolerate string/number differences from backend
    const id1 = o1?.id ?? null;
    const id2 = o2?.id ?? null;
    if (id1 == null && id2 == null) {
      return true;
    }
    if (id1 == null || id2 == null) {
      return false;
    }
    return Number(id1) === Number(id2);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.selectedFile = file;
      this.uploadError = null;

      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.uploadError = 'Vui lòng chọn file ảnh (JPG, PNG, GIF, v.v.)';
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.uploadError = 'Kích thước file không được vượt quá 5MB';
        return;
      }

      // Upload file
      this.uploadImage(file);
    }
  }

  uploadImage(file: File): void {
    this.isUploading = true;
    this.uploadError = null;

    const formData = new FormData();
    formData.append('file', file);

    this.productService.uploadImage(formData).subscribe({
      next: (response: any) => {
        this.isUploading = false;
        if (response.imageUrl) {
          // Update form with uploaded image URL
          this.editForm.patchValue({ imageUrl: response.imageUrl });
          this.notify.success('Tải ảnh lên thành công!');
        }
      },
      error: (error: any) => {
        this.isUploading = false;
        const errorMessage = error.error?.error ?? 'Không thể tải ảnh lên. Vui lòng thử lại.';
        this.uploadError = errorMessage;
        this.notify.error(errorMessage);
      },
    });
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'https://via.placeholder.com/300x300?text=No+Image';
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IProduct>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: (error: HttpErrorResponse) => {
        console.error('Error saving product:', error);
        this.onSaveError(error);
      },
    });
  }

  protected onSaveSuccess(): void {
    this.notify.success('Lưu sản phẩm thành công!');
    this.previousState();
  }

  protected onSaveError(error: HttpErrorResponse): void {
    const errorMessage = error.error?.detail ?? error.message ?? 'Lưu sản phẩm thất bại.';
    this.notify.error(errorMessage);
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(product: IProduct): void {
    console.warn('DEBUG: Product received:', product);
    console.warn('DEBUG: Product category:', product.category);
    console.warn('DEBUG: Available categories:', this.categories);

    // Patch tất cả giá trị trừ category
    this.editForm.patchValue({
      id: product.id,
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity,
      imageUrl: product.imageUrl,
      salesCount: product.salesCount,
    });

    // Find the matching category from the loaded categories list to ensure proper object reference
    let categoryToSet: ICategory | null = null;
    const prodCatId = product.category?.id ?? null;
    if (prodCatId != null) {
      const matchingCategory = this.categories.find(c => Number(c.id) === Number(prodCatId));
      console.warn('DEBUG: Matching category found:', matchingCategory);
      if (matchingCategory) {
        // Use the category from our loaded list to ensure proper object reference
        categoryToSet = matchingCategory;
      }
    }

    console.warn('DEBUG: Category to set:', categoryToSet);

    // Set category separately to ensure proper binding
    this.editForm.controls.category.setValue(categoryToSet);

    console.warn('DEBUG: Form category after setValue:', this.editForm.controls.category.value);
  }

  protected createFromForm(): IProduct | NewProduct {
    const rawValue = this.editForm.getRawValue();
    const category = rawValue.category ?? null;

    // Only send category id to avoid circular reference issues
    const categoryToSend = category ? { id: category.id } : null;

    const product: IProduct | NewProduct = {
      id: rawValue.id,
      name: rawValue.name,
      description: rawValue.description,
      price: rawValue.price,
      quantity: rawValue.quantity,
      imageUrl: rawValue.imageUrl,
      salesCount: rawValue.salesCount,
      category: categoryToSend,
    } as IProduct | NewProduct;

    return product;
  }
}
