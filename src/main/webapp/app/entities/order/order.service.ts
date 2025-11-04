import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
// import { IOrder, getOrderIdentifier } from './order.model'; // Sẽ tạo model sau

export type EntityResponseType = HttpResponse<any>; // Tạm thời dùng any
export type EntityArrayResponseType = HttpResponse<any[]>; // Tạm thời dùng any

@Injectable({ providedIn: 'root' })
export class OrderService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/orders');
  protected myOrdersUrl = this.applicationConfigService.getEndpointFor('api/my-orders');

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<any[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  queryMyOrders(): Observable<EntityArrayResponseType> {
    return this.http
      .get<any[]>(this.myOrdersUrl, { observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((order: any) => {
        order.orderDate = order.orderDate ? dayjs(order.orderDate) : undefined;
      });
    }
    return res;
  }
}
