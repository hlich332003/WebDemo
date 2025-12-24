import { Component, OnInit, OnDestroy, inject, ApplicationRef } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { takeUntil, filter, take } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { WebSocketService } from 'app/shared/services/websocket.service';
import FooterComponent from '../footer/footer.component';
import { ChatWidgetComponent } from '../chat-widget/chat-widget.component';

@Component({
  selector: 'jhi-main',
  standalone: true,
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.scss'],
  imports: [CommonModule, RouterOutlet, FooterComponent, ChatWidgetComponent],
})
export default class MainComponent implements OnInit, OnDestroy {
  showChatWidget = false;

  private readonly accountService = inject(AccountService);
  private readonly authServerProvider = inject(AuthServerProvider);
  private readonly webSocketService = inject(WebSocketService);
  private readonly appRef = inject(ApplicationRef);
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Subscribe to authentication state and connect WS only after account is present and app is stable
    this.accountService
      .getAuthenticationState()
      .pipe(
        filter(account => !!account),
        takeUntil(this.destroy$),
      )
      .subscribe(account => {
        // account is guaranteed to be non-null here due to filter

        // Show chat widget for regular users
        this.showChatWidget = !this.accountService.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_CSKH']);

        // Connect WebSocket once app is stable and account exists
        const token = this.authServerProvider.getToken();
        if (!token) return;

        const isUser = !account.authorities.includes('ROLE_ADMIN') && !account.authorities.includes('ROLE_CSKH');

        if (!this.webSocketService.isConnected) {
          // If regular user, connect immediately (after app stable)
          if (isUser) {
            this.appRef.isStable
              .pipe(
                filter(stable => stable),
                take(1),
              )
              .subscribe(() => {
                Promise.resolve().then(() => {
                  try {
                    this.webSocketService.connect(token, true);
                  } catch (e) {
                    console.error('WebSocket connect failed in MainComponent for USER', e);
                  }
                });
              });
          } else {
            // Admin/CSKH: wait for explicit adminReady signal before connecting
            this.accountService
              .adminReady()
              .pipe(take(1))
              .subscribe(ready => {
                if (!ready) return;
                this.appRef.isStable
                  .pipe(
                    filter(stable => stable),
                    take(1),
                  )
                  .subscribe(() => {
                    Promise.resolve().then(() => {
                      try {
                        // Admin doesn't subscribe user queue
                        this.webSocketService.connect(token, false);
                      } catch (e) {
                        console.error('WebSocket connect failed in MainComponent for ADMIN', e);
                      }
                    });
                  });
              });
          }
        }
      });

    // Also listen for logout case to update UI
    this.accountService
      .getAuthenticationState()
      .pipe(
        filter(account => !account),
        takeUntil(this.destroy$),
      )
      .subscribe(() => {
        this.showChatWidget = false;
        // Do not disconnect here; logout flow will call disconnect explicitly.
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    // Do not disconnect here; keep control in LoginService.logout()
  }
}
