import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IProduct } from '../product.model';
import { ProductService } from '../product.service';

export const productResolve = (route: ActivatedRouteSnapshot): Observable<IProduct | null> => {
  const id = route.params['id'];
  if (id) {
    return inject(ProductService)
      .find(Number(id))
      .pipe(
        mergeMap((product: HttpResponse<IProduct>) => {
          if (product.body) {
            return of(product.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  // Trả về null cho route 'new' để component tạo form trống
  return of(null);
};
