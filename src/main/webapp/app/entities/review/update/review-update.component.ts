import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IUser } from 'app/entities/user/user.model';
import { UserService } from 'app/entities/user/user.service';
import { IProduct } from 'app/entities/product/product.model';
import { ProductService } from 'app/entities/product/product.service';
import { IReview } from '../review.model';
import { ReviewService } from '../review.service';
import { ReviewFormService, ReviewFormGroup } from './review-form.service';

@Component({
  standalone: true,
  selector: 'jhi-review-update',
  templateUrl: './review-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class ReviewUpdateComponent implements OnInit {
  isSaving = false;
  review: IReview | null = null;
  usersSharedCollection: IUser[] = [];
  productsSharedCollection: IProduct[] = [];

  editForm: ReviewFormGroup;

  protected reviewService = inject(ReviewService);
  protected reviewFormService = inject(ReviewFormService);
  protected userService = inject(UserService);
  protected productService = inject(ProductService);
  protected activatedRoute = inject(ActivatedRoute);

  constructor() {
    this.editForm = this.reviewFormService.createReviewFormGroup();
  }

  compareUser = (o1: IUser | null, o2: IUser | null): boolean =>
    this.userService.compareUser(o1, o2);

  compareProduct = (o1: IProduct | null, o2: IProduct | null): boolean =>
    this.productService.compareProduct(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ review }) => {
      this.review = review;
      if (review) {
        this.updateForm(review);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const review = this.reviewFormService.getReview(this.editForm);
    if (review.id !== null) {
      this.subscribeToSaveResponse(this.reviewService.update(review));
    } else {
      this.subscribeToSaveResponse(this.reviewService.create(review));
    }
  }

  protected subscribeToSaveResponse(
    result: Observable<HttpResponse<IReview>>,
  ): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(review: IReview): void {
    this.review = review;
    this.reviewFormService.resetForm(this.editForm, review);

    this.usersSharedCollection = this.userService.addUserToCollectionIfMissing(
      this.usersSharedCollection,
      review.user,
    );
    this.productsSharedCollection =
      this.productService.addProductToCollectionIfMissing(
        this.productsSharedCollection,
        review.product,
      );
  }

  protected loadRelationshipsOptions(): void {
    this.userService
      .query()
      .pipe(map((res: HttpResponse<IUser[]>) => res.body ?? []))
      .pipe(
        map((users: IUser[]) =>
          this.userService.addUserToCollectionIfMissing(
            users,
            this.review?.user,
          ),
        ),
      )
      .subscribe((users: IUser[]) => (this.usersSharedCollection = users));

    this.productService
      .query()
      .pipe(map((res: HttpResponse<IProduct[]>) => res.body ?? []))
      .pipe(
        map((products: IProduct[]) =>
          this.productService.addProductToCollectionIfMissing(
            products,
            this.review?.product,
          ),
        ),
      )
      .subscribe(
        (products: IProduct[]) => (this.productsSharedCollection = products),
      );
  }
}
