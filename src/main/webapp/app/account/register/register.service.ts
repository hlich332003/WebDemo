import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';

export interface Registration {
  login: string;
  email: string;
  password?: string;
  langKey?: string;
  phone?: string;
  firstName?: string; // Thêm thuộc tính firstName
  lastName?: string; // Thêm thuộc tính lastName
}

@Injectable({ providedIn: 'root' })
export class RegisterService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  save(registration: Registration): Observable<{}> {
    return this.http.post(
      this.applicationConfigService.getEndpointFor('api/register'),
      registration,
    );
  }
}
