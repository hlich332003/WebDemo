import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { IOrderSearchDTO } from 'app/shared/model/order-search-dto.model';
import { IOrder } from '../order.model'; // Sửa đường dẫn đúng

@Injectable({ providedIn: 'root' })
export class OrderManagementService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/admin/orders');

  constructor(
    protected http: HttpClient,
    protected applicationConfigService: ApplicationConfigService,
  ) {}

  searchOrders(searchTerm: string): Observable<IOrderSearchDTO[]> {
    return this.http.get<IOrderSearchDTO[]>(`${this.resourceUrl}/search`, {
      params: { searchTerm },
    });
  }

  // Bạn có thể thêm các phương thức khác để quản lý đơn hàng ở đây nếu cần
  // Ví dụ:
  // query(req?: any): Observable<HttpResponse<IOrder[]>> {
  //   const options = createRequestOption(req);
  //   return this.http.get<IOrder[]>(this.resourceUrl, { params: options, observe: 'response' });
  // }
}
