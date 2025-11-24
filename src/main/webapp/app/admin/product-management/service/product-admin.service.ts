import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { IProductSearchDTO } from 'app/shared/model/product-search-dto.model'; // Import interface DTO

@Injectable({ providedIn: 'root' })
export class ProductAdminService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/admin/products');

  constructor(
    protected http: HttpClient,
    protected applicationConfigService: ApplicationConfigService,
  ) {}

  searchProducts(searchTerm: string): Observable<IProductSearchDTO[]> {
    return this.http.get<IProductSearchDTO[]>(`${this.resourceUrl}/search`, { params: { searchTerm } });
  }

  // Bạn có thể thêm các phương thức khác để quản lý sản phẩm ở đây nếu cần
}
