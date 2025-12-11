import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IProduct } from '../product.model';
import { ProductService } from '../product.service';

export const productResolve = (
  route: ActivatedRouteSnapshot,
): Observable<NonNullable<IProduct>> => {
  const id = route.params['id'];
  if (id) {
    return (inject(ProductService) as ProductService).find(Number(id)).pipe(
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
  // Trả về một sản phẩm hợp lệ với các thuộc tính bắt buộc khi không có ID
  // Hoặc điều hướng đến trang lỗi nếu không muốn hiển thị sản phẩm rỗng
  // Hiện tại, tôi sẽ điều hướng đến trang lỗi nếu không có ID
  inject(Router).navigate(['404']);
  return EMPTY;
};
