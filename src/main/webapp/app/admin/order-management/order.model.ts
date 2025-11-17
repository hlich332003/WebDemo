import { Account } from 'app/core/auth/account.model';
import { IOrderItem } from './order-item.model';
// import dayjs from 'dayjs/esm'; // Không cần import dayjs ở đây nữa

export interface IOrder {
  id?: number;
  orderCode?: string | null;
  orderDate?: Date | null; // Đã sửa kiểu thành Date | null
  totalAmount?: number | null;
  status?: string | null;
  customerFullName?: string | null; // Thêm trường này
  customerEmail?: string | null;
  customerPhone?: string | null; // Thêm trường này
  deliveryAddress?: string | null;
  paymentMethod?: string | null;
  notes?: string | null;
  customer?: Account | null;
  items?: IOrderItem[] | null;
}

export type NewOrder = Omit<IOrder, 'id'> & { id: null };
