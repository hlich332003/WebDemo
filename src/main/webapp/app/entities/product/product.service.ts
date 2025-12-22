import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IProduct, NewProduct } from './product.model';
import { ICategory } from 'app/entities/category/category.model';

export type PartialUpdateProduct = Partial<IProduct> & Pick<IProduct, 'id'>;

type RestOf<T extends IProduct | NewProduct | PartialUpdateProduct> = Omit<
  T,
  'createdDate' | 'lastModifiedDate'
> & {
  createdDate?: string | null;
  lastModifiedDate?: string | null;
};

export type RestProduct = RestOf<IProduct>;
export type NewRestProduct = RestOf<NewProduct>;
export type PartialUpdateRestProduct = RestOf<PartialUpdateProduct>;

export type EntityResponseType = HttpResponse<IProduct>;
export type EntityArrayResponseType = HttpResponse<IProduct[]>;

@Injectable({ providedIn: 'root' })
export class ProductService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl =
    this.applicationConfigService.getEndpointFor('api/products');
  protected categoryResourceUrl =
    this.applicationConfigService.getEndpointFor('api/categories');

  create(product: NewProduct): Observable<EntityResponseType> {
    return this.http.post<IProduct>(this.resourceUrl, product, {
      observe: 'response',
    });
  }

  update(product: IProduct): Observable<EntityResponseType> {
    return this.http.put<IProduct>(
      `${this.resourceUrl}/${product.id}`,
      product,
      {
        observe: 'response',
      },
    );
  }

  partialUpdate(product: PartialUpdateProduct): Observable<EntityResponseType> {
    return this.http.patch<IProduct>(
      `${this.resourceUrl}/${product.id}`,
      product,
      {
        observe: 'response',
      },
    );
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IProduct>(`${this.resourceUrl}/${id}`, {
      observe: 'response',
    });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IProduct[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, {
      observe: 'response',
    });
  }

  getCategories(): Observable<HttpResponse<ICategory[]>> {
    return this.http.get<ICategory[]>(this.categoryResourceUrl, {
      observe: 'response',
    });
  }

  compareProduct(o1: IProduct | null, o2: IProduct | null): boolean {
    return o1 && o2 ? o1.id === o2.id : o1 === o2;
  }

  addProductToCollectionIfMissing<Type extends Pick<IProduct, 'id'>>(
    productCollection: Type[],
    ...productsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const products: Type[] = productsToCheck.filter(isPresent);
    if (products.length > 0) {
      const productCollectionIdentifiers = productCollection.map(
        (productItem) => productItem.id,
      );
      const productsToAdd = products.filter((productItem) => {
        if (
          productItem.id == null ||
          productCollectionIdentifiers.includes(productItem.id)
        ) {
          return false;
        }
        productCollectionIdentifiers.push(productItem.id);
        return true;
      });
      return [...productsToAdd, ...productCollection];
    }
    return productCollection;
  }

  toggleFeatured(id: number): Observable<HttpResponse<{}>> {
    return this.http.patch(`${this.resourceUrl}/${id}/toggle-featured`, null, {
      observe: 'response',
    });
  }
}
