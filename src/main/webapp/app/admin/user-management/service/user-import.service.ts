import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from 'app/core/config/application-config.service';

@Injectable({ providedIn: 'root' })
export class UserImportService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor(
    'api/admin/users/import',
  );

  importUsers(file: File): Observable<HttpResponse<{}>> {
    const formData: FormData = new FormData();
    formData.append('file', file, file.name);

    return this.http.post(this.resourceUrl, formData, {
      observe: 'response',
    });
  }
}
