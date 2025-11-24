import dayjs from 'dayjs';

export interface IOrderSearchDTO {
  id?: number;
  customerLogin?: string;
  orderDate?: dayjs.Dayjs; // Sử dụng dayjs cho Instant
  totalAmount?: number;
  status?: string;
}

export type NewOrderSearchDTO = Omit<IOrderSearchDTO, 'id'> & { id: null };
