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
  private readonly destroy$ = new Subject<never>();

  ngOnInit(): void {
    // FIX: Chủ động kiểm tra danh tính user khi reload trang
    this.accountService.identity().subscribe();

    // 1. Lắng nghe trạng thái đăng nhập để hiển thị Chat Widget
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        if (account) {
          // User đã đăng nhập
          this.showChatWidget = !this.accountService.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_CSKH']);
          const token = this.authServerProvider.getToken();
          if (token && !this.webSocketService.isConnected) {
            console.warn('[MainComponent] User logged in -> Connecting WebSocket...');
            // FIX: Pass endpoint as first param, token as second param
            this.webSocketService.connect('/websocket', token);

            // Subscribe to notifications after connection
            this.webSocketService.onConnected().subscribe(() => {
              console.warn('[MainComponent] ✅ WebSocket connected, subscribing to notifications...');
              this.webSocketService.subscribe('/user/queue/notifications', (notification: any) => {
                console.warn('[MainComponent] 🔔 Received notification:', notification);
                this.webSocketService.pushNotification(notification);
              });
            });
          }
        } else {
          // Guest
          this.showChatWidget = true; // Luôn hiện chat widget cho guest
          if (!this.webSocketService.isConnected) {
            console.warn('[MainComponent] Guest session -> Connecting WebSocket...');
            this.webSocketService.connect('/websocket'); // Connect không cần token
          }
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next(undefined as never);
    this.destroy$.complete();
  }
}
