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
import { AccountService } from 'app/core/auth/account.service';

export type PartialUpdateProduct = Partial<IProduct> & Pick<IProduct, 'id'>;

type RestOf<T extends IProduct | NewProduct | PartialUpdateProduct> = Omit<T, 'createdDate' | 'lastModifiedDate'> & {
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
  private accountService = inject(AccountService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/products');
  protected publicResourceUrl = this.applicationConfigService.getEndpointFor('api/public/products');
  protected categoryResourceUrl = this.applicationConfigService.getEndpointFor('api/categories');
  protected publicCategoryResourceUrl = this.applicationConfigService.getEndpointFor('api/public/categories');

  create(product: NewProduct): Observable<EntityResponseType> {
    return this.http.post<IProduct>(this.resourceUrl, product, {
      observe: 'response',
    });
  }

  update(product: IProduct): Observable<EntityResponseType> {
    return this.http.put<IProduct>(`${this.resourceUrl}/${product.id}`, product, {
      observe: 'response',
    });
  }

  partialUpdate(product: PartialUpdateProduct): Observable<EntityResponseType> {
    return this.http.patch<IProduct>(`${this.resourceUrl}/${product.id}`, product, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    const url = this.accountService.isAuthenticated() ? `${this.resourceUrl}/${id}` : `${this.publicResourceUrl}/${id}`;
    return this.http.get<IProduct>(url, {
      observe: 'response',
    });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    const url = this.accountService.isAuthenticated() ? this.resourceUrl : this.publicResourceUrl;
    return this.http.get<IProduct[]>(url, {
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
    const url = this.accountService.isAuthenticated() ? this.categoryResourceUrl : this.publicCategoryResourceUrl;
    return this.http.get<ICategory[]>(url, {
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
      const productCollectionIdentifiers = productCollection.map(productItem => productItem.id);
      const productsToAdd = products.filter(productItem => {
        if (productItem.id == null || productCollectionIdentifiers.includes(productItem.id)) {
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
