import { IOrder } from 'app/entities/order/order.model';
import { Dayjs } from 'dayjs';

export interface IPayment {
  id: number;
  method?: string | null;
  status?: string | null;
  paidAt?: Dayjs | null;
  amount?: number | null;
  order?: Pick<IOrder, 'id'> | null;
}

export type NewPayment = Omit<IPayment, 'id'> & { id: null };
