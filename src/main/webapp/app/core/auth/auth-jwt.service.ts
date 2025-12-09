import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, map } from 'rxjs/operators';

import { Login } from 'app/login/login.model';
import { ApplicationConfigService } from '../config/application-config.service';
import { StateStorageService } from './state-storage.service';

type JwtToken = {
  id_token: string;
};

@Injectable({ providedIn: 'root' })
export class AuthServerProvider {
  private readonly http = inject(HttpClient);
  private readonly stateStorageService = inject(StateStorageService);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  getToken(): string {
    return this.stateStorageService.getAuthenticationToken() ?? '';
  }

  login(credentials: Login): Observable<void> {
    return this.http
      .post<JwtToken>(
        this.applicationConfigService.getEndpointFor('api/authenticate'),
        credentials,
      )
      .pipe(
        tap((response) =>
          this.authenticateSuccess(response, credentials.rememberMe),
        ),
        map(() => undefined),
      );
  }

  logout(): Observable<void> {
    const token = this.getToken();

    // Nếu có token, gọi API backend để blacklist
    if (token) {
      return this.http
        .post<void>(
          this.applicationConfigService.getEndpointFor('api/account/logout'),
          {},
        )
        .pipe(
          tap(() => {
            console.log('✅ Token đã được blacklist trên server');
            this.stateStorageService.clearAuthenticationToken();
          }),
          map(() => undefined),
        );
    }

    // Nếu không có token, chỉ clear local storage
    return new Observable((observer) => {
      this.stateStorageService.clearAuthenticationToken();
      observer.complete();
    });
  }

  private authenticateSuccess(response: JwtToken, rememberMe: boolean): void {
    console.log(
      '🔐 Storing token:',
      response.id_token.substring(0, 20) + '...',
      'rememberMe:',
      rememberMe,
    );
    this.stateStorageService.storeAuthenticationToken(
      response.id_token,
      rememberMe,
    );
    console.log('✅ Token stored. Checking storage...');
    console.log(
      'sessionStorage:',
      sessionStorage.getItem('jhi-authenticationToken'),
    );
    console.log(
      'localStorage:',
      localStorage.getItem('jhi-authenticationToken'),
    );
  }
}
