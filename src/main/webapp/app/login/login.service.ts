import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { mergeMap, tap } from 'rxjs/operators';

import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { WebSocketService } from 'app/shared/services/websocket.service';
import { Login } from './login.model';

@Injectable({ providedIn: 'root' })
export class LoginService {
  private readonly accountService = inject(AccountService);
  private readonly authServerProvider = inject(AuthServerProvider);
  private readonly webSocketService = inject(WebSocketService);

  login(credentials: Login): Observable<Account | null> {
    return this.authServerProvider.login(credentials).pipe(
      // After login succeeds and token is stored, fetch account identity – no WebSocket side-effect here
      mergeMap(() => this.accountService.identity(true)),
      // After identity is loaded, connect websocket for user if token exists
      tap(() => {
        try {
          const token = this.authServerProvider.getToken();
          if (token) {
            // Connect the user websocket after token stored
            this.webSocketService.connect('/websocket', token);
          }
        } catch (e) {
          // ignore connection errors here
        }
      }),
    );
  }

  logout(): void {
    this.webSocketService.disconnect();
    this.authServerProvider.logout().subscribe({ complete: () => this.accountService.authenticate(null) });
  }
}
