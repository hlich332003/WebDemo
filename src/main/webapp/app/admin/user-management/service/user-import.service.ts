import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { User } from '../user-management.model';
import * as XLSX from 'xlsx';

@Injectable({ providedIn: 'root' })
export class UserImportService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/admin/users/import');

  importUsers(file: File): Observable<HttpResponse<{}>> {
    const formData: FormData = new FormData();
    formData.append('file', file, file.name);

    return this.http.post(this.resourceUrl, formData, {
      observe: 'response',
    });
  }

  exportUsers(users: User[]): Observable<void> {
    return new Observable<void>(observer => {
      try {
        // Chuẩn bị dữ liệu cho Excel
        const exportData = users.map(user => ({
          ID: user.id,
          Email: user.email,
          'Ho ten': `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim(),
          'Dien thoai': user.phone ?? '',
          'Trang thai': user.activated ? 'Da kich hoat' : 'Chua kich hoat',
          'Vai tro': user.authorities?.join(', ') ?? '',
          'Ngay tao': user.createdDate ? new Date(user.createdDate).toLocaleString('vi-VN') : '',
        }));

        // Tạo worksheet và workbook
        const ws = XLSX.utils.json_to_sheet(exportData);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, 'Danh sach nguoi dung');

        // Xuất file
        const fileName = `danh-sach-nguoi-dung-${new Date().getTime()}.xlsx`;
        XLSX.writeFile(wb, fileName);

        observer.next();
        observer.complete();
      } catch (error) {
        observer.error(error);
      }
    });
  }
}
