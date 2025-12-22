import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import dayjs from 'dayjs/esm';

import { IPayment, NewPayment } from '../payment.model';

type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<
  Omit<T, 'id'>
> & { id: T['id'] };
type PaymentFormGroupInput = IPayment | PartialWithRequiredKeyOf<NewPayment>;
type FormValueOf<T extends IPayment | NewPayment> = Omit<T, 'paidAt'> & {
  paidAt?: string | null;
};

type PaymentFormRawValue = FormValueOf<IPayment>;
type NewPaymentFormRawValue = FormValueOf<NewPayment>;
type PaymentFormDefaults = Pick<NewPayment, 'id' | 'paidAt'>;
type PaymentFormGroupContent = {
  id: FormControl<PaymentFormRawValue['id'] | NewPayment['id']>;
  method: FormControl<PaymentFormRawValue['method']>;
  status: FormControl<PaymentFormRawValue['status']>;
  paidAt: FormControl<PaymentFormRawValue['paidAt']>;
  amount: FormControl<PaymentFormRawValue['amount']>;
  order: FormControl<PaymentFormRawValue['order']>;
};

export type PaymentFormGroup = FormGroup<PaymentFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class PaymentFormService {
  createPaymentFormGroup(
    payment: PaymentFormGroupInput = { id: null },
  ): PaymentFormGroup {
    const paymentRawValue = this.convertPaymentToPaymentRawValue({
      ...this.getFormDefaults(),
      ...payment,
    });
    return new FormGroup<PaymentFormGroupContent>({
      id: new FormControl(
        { value: paymentRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      method: new FormControl(paymentRawValue.method),
      status: new FormControl(paymentRawValue.status),
      paidAt: new FormControl(paymentRawValue.paidAt),
      amount: new FormControl(paymentRawValue.amount),
      order: new FormControl(paymentRawValue.order),
    });
  }

  getPayment(form: PaymentFormGroup): IPayment | NewPayment {
    return this.convertPaymentRawValueToPayment(
      form.getRawValue() as PaymentFormRawValue | NewPaymentFormRawValue,
    );
  }

  resetForm(form: PaymentFormGroup, payment: PaymentFormGroupInput): void {
    const paymentRawValue = this.convertPaymentToPaymentRawValue({
      ...this.getFormDefaults(),
      ...payment,
    });
    form.reset({
      ...paymentRawValue,
      id: { value: paymentRawValue.id, disabled: true },
    } as any);
  }

  private getFormDefaults(): PaymentFormDefaults {
    return {
      id: null,
      paidAt: dayjs(),
    };
  }

  private convertPaymentToPaymentRawValue(
    payment: IPayment | (Partial<NewPayment> & PaymentFormDefaults),
  ): PaymentFormRawValue | PartialWithRequiredKeyOf<NewPaymentFormRawValue> {
    return {
      ...payment,
      paidAt: payment.paidAt?.format('YYYY-MM-DDTHH:mm') ?? null,
    };
  }

  private convertPaymentRawValueToPayment(
    rawPayment: PaymentFormRawValue | NewPaymentFormRawValue,
  ): IPayment | NewPayment {
    return {
      ...rawPayment,
      paidAt: rawPayment.paidAt ? dayjs(rawPayment.paidAt) : null,
    };
  }
}
