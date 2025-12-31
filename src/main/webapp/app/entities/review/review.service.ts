import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IReview, NewReview } from './review.model';

export type PartialUpdateReview = Partial<IReview> & Pick<IReview, 'id'>;

type RestOf<T extends IReview | NewReview> = Omit<T, 'createdDate'> & {
  createdDate?: string | null;
};

export type RestReview = RestOf<IReview>;

export type NewRestReview = RestOf<NewReview>;

export type PartialUpdateRestReview = RestOf<PartialUpdateReview>;

export type EntityResponseType = HttpResponse<IReview>;
export type EntityArrayResponseType = HttpResponse<IReview[]>;

@Injectable({ providedIn: 'root' })
export class ReviewService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/reviews');

  create(review: NewReview): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(review);
    return this.http
      .post<RestReview>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(review: IReview): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(review);
    return this.http
      .put<RestReview>(`${this.resourceUrl}/${review.id}`, copy, {
        observe: 'response',
      })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestReview>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestReview[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, {
      observe: 'response',
    });
  }

  protected convertDateFromClient<T extends IReview | NewReview | PartialUpdateReview>(review: T): RestOf<T> {
    return {
      ...review,
      createdDate: review.createdDate?.toJSON() ?? null,
    };
  }

  protected convertDateFromServer(restReview: RestReview): IReview {
    return {
      ...restReview,
      createdDate: restReview.createdDate ? dayjs(restReview.createdDate) : null,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestReview>): HttpResponse<IReview> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestReview[]>): HttpResponse<IReview[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
