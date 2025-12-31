import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import dayjs from 'dayjs/esm';

import { IReview, NewReview } from '../review.model';

type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };
type ReviewFormGroupInput = IReview | PartialWithRequiredKeyOf<NewReview>;
type FormValueOf<T extends IReview | NewReview> = Omit<T, 'createdDate'> & {
  createdDate?: string | null;
};

type ReviewFormRawValue = FormValueOf<IReview>;
type NewReviewFormRawValue = FormValueOf<NewReview>;
type ReviewFormDefaults = Pick<NewReview, 'id' | 'createdDate'>;
type ReviewFormGroupContent = {
  id: FormControl<ReviewFormRawValue['id'] | NewReview['id']>;
  rating: FormControl<ReviewFormRawValue['rating']>;
  comment: FormControl<ReviewFormRawValue['comment']>;
  createdDate: FormControl<ReviewFormRawValue['createdDate']>;
  user: FormControl<ReviewFormRawValue['user']>;
  product: FormControl<ReviewFormRawValue['product']>;
};

export type ReviewFormGroup = FormGroup<ReviewFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ReviewFormService {
  createReviewFormGroup(review: ReviewFormGroupInput = { id: null }): ReviewFormGroup {
    const reviewRawValue = this.convertReviewToReviewRawValue({
      ...this.getFormDefaults(),
      ...review,
    });
    return new FormGroup<ReviewFormGroupContent>({
      id: new FormControl(
        { value: reviewRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      rating: new FormControl(reviewRawValue.rating, {
        validators: [Validators.required, Validators.min(1), Validators.max(5)],
      }),
      comment: new FormControl(reviewRawValue.comment),
      createdDate: new FormControl(reviewRawValue.createdDate),
      user: new FormControl(reviewRawValue.user),
      product: new FormControl(reviewRawValue.product),
    });
  }

  getReview(form: ReviewFormGroup): IReview | NewReview {
    return this.convertReviewRawValueToReview(form.getRawValue() as ReviewFormRawValue | NewReviewFormRawValue);
  }

  resetForm(form: ReviewFormGroup, review: ReviewFormGroupInput): void {
    const reviewRawValue = this.convertReviewToReviewRawValue({
      ...this.getFormDefaults(),
      ...review,
    });
    form.reset({
      ...reviewRawValue,
      id: { value: reviewRawValue.id, disabled: true },
    } as any);
  }

  private getFormDefaults(): ReviewFormDefaults {
    return {
      id: null,
      createdDate: dayjs(),
    };
  }

  private convertReviewToReviewRawValue(
    review: IReview | (Partial<NewReview> & ReviewFormDefaults),
  ): ReviewFormRawValue | PartialWithRequiredKeyOf<NewReviewFormRawValue> {
    return {
      ...review,
      createdDate: review.createdDate?.format('YYYY-MM-DDTHH:mm') ?? null,
    };
  }

  private convertReviewRawValueToReview(rawReview: ReviewFormRawValue | NewReviewFormRawValue): IReview | NewReview {
    return {
      ...rawReview,
      createdDate: rawReview.createdDate ? dayjs(rawReview.createdDate) : null,
    };
  }
}
