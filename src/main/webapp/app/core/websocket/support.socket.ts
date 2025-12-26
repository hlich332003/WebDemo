import { Injectable, inject } from '@angular/core';
import { WebSocketService } from 'app/shared/services/websocket.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { BehaviorSubject, Subscription, Observable } from 'rxjs';
import { WS_CONFIG } from './websocket.config';
import { INotification } from 'app/shared/model/notification.model';

@Injectable({ providedIn: 'root' })
export class SupportSocket {
  // Public observable to let components react on connection state if needed
  public readonly isConnected$: Observable<boolean>;

  // Internal subject backing the public observable
  private connected$ = new BehaviorSubject<boolean>(false);

  // Keep subscriptions so we can unsubscribe cleanly on disconnect
  private connectionSub?: Subscription;
  // we will not rely on a returned Subscription from wsService.subscribe (it may return void),
  // use wsService.unsubscribe(...) on disconnect instead
  private notificationSubscribed = false;

  // Injected services
  private wsService = inject(WebSocketService);
  private storage = inject(StateStorageService);

  constructor() {
    this.isConnected$ = this.connected$.asObservable();
  }

  connect(endpoint?: string, token?: string): void {
    // Use shared WebSocketService to manage single connection
    // If shared service already connected, avoid re-connecting
    if ((this.wsService as any).isConnected) {
      // ensure we are subscribed to notifications and connection state
      this.ensureSubscriptions();
      return;
    }

    const providedToken = token ?? this.storage.getAuthenticationToken();
    if (!providedToken) {
      console.warn('[SupportSocket] No token, skipping connection');
      return;
    }

    // connect service (should be idempotent)
    this.wsService.connect(endpoint ?? '/websocket', providedToken);

    // Ensure we subscribe to connection state and notifications once
    this.ensureSubscriptions();
  }

  sendReply(conversationId: number, content: string): void {
    // Delegate to shared service which ensures connection & publish
    this.wsService.sendReplyAsCskh(conversationId, content);
  }

  disconnect(): void {
    // Unsubscribe admin notification by calling shared service unsubscribe
    try {
      if (this.notificationSubscribed) {
        try {
          this.wsService.unsubscribe(WS_CONFIG.BROKER.ADMIN_NOTIFICATIONS);
        } catch (e) {
          // noop
        }
        this.notificationSubscribed = false;
      }
    } catch (e) {
      /* noop */
    }

    // Unsubscribe connection state subscription
    try {
      this.connectionSub?.unsubscribe();
    } catch (e) {
      /* noop */
    }
    this.connectionSub = undefined;

    // Do not force disconnect the shared service here; leave lifecycle to auth/logout flows
    this.connected$.next(false);
  }

  // The shared WebSocketService handles stomp errors and close events centrally
  // Private helpers
  private ensureSubscriptions(): void {
    // Connection state subscription: keep one subscription
    this.connectionSub ??= this.wsService.connectionState$.subscribe((v: boolean) => {
      this.connected$.next(v);
    });

    // Admin notifications: subscribe once
    if (!this.notificationSubscribed) {
      // Call subscribe (may return void). We don't assume a returned Subscription.
      try {
        this.wsService.subscribe(WS_CONFIG.BROKER.ADMIN_NOTIFICATIONS, (payload: any) => {
          try {
            const notification: INotification = {
              type: payload?.type ?? 'SYSTEM',
              title: payload?.title ?? payload?.message,
              content: payload?.content ?? payload?.message,
              link: payload?.link,
              timestamp: payload?.timestamp ? new Date(payload.timestamp) : new Date(),
            };
            this.wsService.pushNotification(notification);
          } catch (err) {
            console.error('[SupportSocket] Notification parse error', err);
          }
        });
        this.notificationSubscribed = true;
      } catch (err) {
        console.error('[SupportSocket] Failed to subscribe admin notifications', err);
      }
    }
  }
}
