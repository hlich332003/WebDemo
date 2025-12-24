import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';

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
    return this.http.post<JwtToken>(this.applicationConfigService.getEndpointFor('api/authenticate'), credentials).pipe(
      tap(response => this.authenticateSuccess(response, credentials.rememberMe)),
      map(() => undefined),
    );
  }

  logout(): Observable<void> {
    const token = this.getToken();

    const logoutAndCleanup = () => {
      this.stateStorageService.clearAuthenticationToken();
    };

    if (token) {
      const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
      return this.http.post<void>(this.applicationConfigService.getEndpointFor('api/account/logout'), {}, { headers }).pipe(
        tap(() => {
          console.log('✅ Token has been blacklisted on the server');
          logoutAndCleanup();
        }),
        catchError(error => {
          console.error('Error blacklisting token, logging out client-side anyway.', error);
          logoutAndCleanup();
          return of(undefined); // Ensure the stream completes
        }),
        map(() => undefined),
      );
    }

    logoutAndCleanup();
    return of(undefined);
  }

  private authenticateSuccess(response: JwtToken, rememberMe: boolean): void {
    console.log('🔐 Storing token:', response.id_token.substring(0, 20) + '...', 'rememberMe:', rememberMe);
    this.stateStorageService.storeAuthenticationToken(response.id_token, rememberMe);
    console.log('✅ Token stored. Checking storage...');
    console.log('sessionStorage:', sessionStorage.getItem('jhi-authenticationToken'));
    console.log('localStorage:', localStorage.getItem('jhi-authenticationToken'));
  }
}
