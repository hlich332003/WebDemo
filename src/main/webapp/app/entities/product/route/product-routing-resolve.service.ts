import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IProduct } from '../product.model';
import { ProductService } from '../product.service'; // Sửa đường dẫn import

export const productResolve = (
  route: ActivatedRouteSnapshot,
): Observable<IProduct | null | never> => {
  const id = route.params['id'];
  if (id) {
    return inject(ProductService)
      .find(id)
      .pipe(
        mergeMap((product: HttpResponse<IProduct>) => {
          if (product.body) {
            return of(product.body);
          }
          return EMPTY;
        }),
      );
  }
  return of(null);
};

export default productResolve;
