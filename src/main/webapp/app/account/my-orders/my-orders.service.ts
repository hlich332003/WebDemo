import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { IOrder } from 'app/admin/order-management/order.model'; // Sử dụng lại IOrder model

export type EntityArrayResponseType = HttpResponse<IOrder[]>;

@Injectable({ providedIn: 'root' })
export class MyOrdersService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/my-orders'); // Giả định API endpoint cho đơn hàng của người dùng

  query(): Observable<EntityArrayResponseType> {
    return this.http
      .get<IOrder[]>(this.resourceUrl, { observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((order: IOrder) => {
        order.orderDate = order.orderDate ? dayjs(order.orderDate).toDate() : null; // Chuyển đổi Dayjs sang Date, hoặc null
      });
    }
    return res;
  }
}
