import { Dayjs } from 'dayjs';

export interface INotification {
  id?: number;
  message?: string;
  read?: boolean;
  timestamp?: Dayjs;
  orderId?: number;
  link?: string;
}

export class Notification implements INotification {
  constructor(
    public id?: number,
    public message?: string,
    public read?: boolean,
    public timestamp?: Dayjs,
    public orderId?: number,
    public link?: string,
  ) {}
}
