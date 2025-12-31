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
      mergeMap(() => this.accountService.identity(true)),
      tap(() => {
        // FIX: Connect WebSocket ngay sau khi login thành công
        const token = this.authServerProvider.getToken();
        if (token && !this.webSocketService.isConnected) {
          console.warn('[LoginService] Login success -> Connecting WebSocket...');
          // FIX: Pass endpoint as first param, token as second param
          this.webSocketService.connect('/websocket', token);
        }
      }),
    );
  }

  logout(): void {
    // FIX: Disconnect WebSocket ngay khi logout

    console.warn('[LoginService] Logout -> Disconnecting WebSocket...');
    this.webSocketService.disconnect();
    this.authServerProvider.logout().subscribe({ complete: () => this.accountService.authenticate(null) });
  }
}
