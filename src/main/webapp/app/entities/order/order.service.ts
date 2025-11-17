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

  create(orderData: any): Observable<EntityResponseType> {
    return this.http.post<any>(this.resourceUrl, orderData, {
      observe: 'response',
      headers: { 'Content-Type': 'application/json; charset=UTF-8' },
    });
  }

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

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<any>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  cancelOrder(id: number): Observable<EntityResponseType> {
    return this.http.patch<any>(`${this.resourceUrl}/${id}/cancel`, null, { observe: 'response' });
  }

  updateDeliveryAddress(id: number, address: string): Observable<EntityResponseType> {
    return this.http.patch<any>(`${this.resourceUrl}/${id}/address`, { address: address }, { observe: 'response' });
  }

  updateStatus(id: number, status: string): Observable<EntityResponseType> {
    return this.http
      .patch<any>(`${this.resourceUrl}/${id}/status`, { status: status }, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.orderDate = res.body.orderDate ? dayjs(res.body.orderDate).toDate() : null; // Đã sửa lỗi
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((order: any) => {
        order.orderDate = order.orderDate ? dayjs(order.orderDate).toDate() : null; // Đã sửa lỗi
      });
    }
    return res;
  }
}
