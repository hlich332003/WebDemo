import { Pipe, PipeTransform } from '@angular/core';
import dayjs from 'dayjs/esm';

@Pipe({
  name: 'notificationDate',
  standalone: true,
})
export class NotificationDatePipe implements PipeTransform {
  transform(value: any): dayjs.Dayjs | null {
    if (!value) {
      return null;
    }
    if (dayjs.isDayjs(value)) {
      return value;
    }
    return dayjs(value);
  }
}
