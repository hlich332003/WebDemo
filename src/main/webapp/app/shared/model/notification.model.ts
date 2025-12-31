import { Dayjs } from 'dayjs';

export interface INotification {
  id?: number;
  type?: string;
  message?: string;
  title?: string;
  content?: string;
  read?: boolean;
  timestamp?: Dayjs | Date;
  orderId?: number;
  link?: string;
}
