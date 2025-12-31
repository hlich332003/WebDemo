import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IPayment, NewPayment } from './payment.model';

export type PartialUpdatePayment = Partial<IPayment> & Pick<IPayment, 'id'>;

type RestOf<T extends IPayment | NewPayment> = Omit<T, 'paidAt'> & {
  paidAt?: string | null;
};

export type RestPayment = RestOf<IPayment>;

export type NewRestPayment = RestOf<NewPayment>;

export type PartialUpdateRestPayment = RestOf<PartialUpdatePayment>;

export type EntityResponseType = HttpResponse<IPayment>;
export type EntityArrayResponseType = HttpResponse<IPayment[]>;

@Injectable({ providedIn: 'root' })
export class PaymentService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/payments');

  create(payment: NewPayment): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(payment);
    return this.http
      .post<RestPayment>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(payment: IPayment): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(payment);
    return this.http
      .put<RestPayment>(`${this.resourceUrl}/${payment.id}`, copy, {
        observe: 'response',
      })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestPayment>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestPayment[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, {
      observe: 'response',
    });
  }

  protected convertDateFromClient<T extends IPayment | NewPayment | PartialUpdatePayment>(payment: T): RestOf<T> {
    return {
      ...payment,
      paidAt: payment.paidAt?.toJSON() ?? null,
    };
  }

  protected convertDateFromServer(restPayment: RestPayment): IPayment {
    return {
      ...restPayment,
      paidAt: restPayment.paidAt ? dayjs(restPayment.paidAt) : null,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestPayment>): HttpResponse<IPayment> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestPayment[]>): HttpResponse<IPayment[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
