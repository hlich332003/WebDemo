import { Dayjs } from 'dayjs';

export interface INotification {
  id?: number;
  type?: string; // optional type to align with service INotification
  message?: string;
  title?: string;
  content?: string;
  read?: boolean;
  timestamp?: Dayjs | Date;
  orderId?: number;
  link?: string;
}

export class Notification implements INotification {
  constructor(
    public id?: number,
    public message?: string,
    public read?: boolean,
    public timestamp?: Dayjs | Date,
    public orderId?: number,
    public link?: string,
    public type?: string,
    public title?: string,
    public content?: string,
  ) {}
}
