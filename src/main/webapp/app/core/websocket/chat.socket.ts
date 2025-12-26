import { Injectable, inject } from '@angular/core';
import { WebSocketService } from 'app/shared/services/websocket.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { BehaviorSubject, Subject, Observable, Subscription } from 'rxjs';
import { WS_CONFIG } from './websocket.config';
import { INotification } from 'app/shared/model/notification.model';

export interface ChatMessage {
  content: string;
  senderType: 'USER' | 'ADMIN' | 'SYSTEM' | 'CSKH';
  timestamp: Date;
  type?: 'MESSAGE' | 'SESSION_ENDED' | 'system';
  conversationId?: number;
  senderIdentifier?: string;
  message?: string;
  isFromAdmin?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ChatSocket {
  // Public observables first (ESLint member-ordering)
  public readonly isConnected$: Observable<boolean>;

  // Private fields
  private wsService = inject(WebSocketService);
  private storage = inject(StateStorageService);

  private connectionSub?: Subscription;
  private messageSub?: Subscription;
  private notificationsSubscribed = false;

  private connected$ = new BehaviorSubject<boolean>(false);
  private message$ = new Subject<ChatMessage>();

  constructor() {
    this.isConnected$ = this.connected$.asObservable();
  }

  // Public methods
  connect(endpoint?: string, token?: string): void {
    // Use shared WebSocketService to ensure single global connection
    if ((this.wsService as any).isConnected) {
      // already connected: ensure subscriptions present
      this.ensureSubscriptions();
      return;
    }

    const providedToken = token ?? this.storage.getAuthenticationToken();
    if (!providedToken) {
      console.warn('[ChatSocket] No token, skipping connection');
      return;
    }

    this.wsService.connect(endpoint ?? '/websocket', providedToken);

    // Subscribe to notifications only; message per-conversation is handled by openChat()
    this.ensureSubscriptions();
  }

  openChat(conversationId: number): void {
    // unsubscribe previous per-conversation subscription to avoid duplicates
    try {
      this.messageSub?.unsubscribe();
    } catch (e) {
      /* noop */
    }

    // delegate to shared service which will subscribe to standardized topic
    this.wsService.openChat(conversationId);

    // relay messages from shared service to local subject
    this.messageSub = this.wsService.chatMessage$.subscribe((m: any) => {
      try {
        const msg: ChatMessage = {
          content: m.content ?? m.message,
          senderType: m.senderType,
          timestamp: m.createdAt ? new Date(m.createdAt) : new Date(),
          type: m.type,
          conversationId: m.conversationId,
          senderIdentifier: m.senderIdentifier,
          message: m.message,
          isFromAdmin: m.senderType === 'CSKH' || m.senderType === 'ADMIN',
        };
        this.message$.next(msg);
      } catch (e) {
        console.error('[ChatSocket] parse message', e);
      }
    });
  }

  sendMessage(content: string): void {
    // delegate to shared service to publish; it will guard connection
    this.wsService.sendMessage({ content });
  }

  disconnect(): void {
    // unsubscribe notifications; do not force global disconnect
    try {
      if (this.notificationsSubscribed) {
        this.wsService.unsubscribe(WS_CONFIG.BROKER.NOTIFICATIONS_QUEUE);
        this.notificationsSubscribed = false;
      }
    } catch (e) {
      /* noop */
    }

    try {
      this.messageSub?.unsubscribe();
    } catch (e) {
      /* noop */
    }

    try {
      this.connectionSub?.unsubscribe();
    } catch (e) {
      /* noop */
    }

    this.connected$.next(false);
  }

  get messages$(): Observable<ChatMessage> {
    return this.message$.asObservable();
  }

  // Private helpers
  private ensureSubscriptions(): void {
    // reflect connection state - keep one subscription
    this.connectionSub ??= this.wsService.connectionState$.subscribe(v => this.connected$.next(v));

    // subscribe to notifications (idempotent)
    if (!this.notificationsSubscribed) {
      try {
        this.wsService.subscribe(WS_CONFIG.BROKER.NOTIFICATIONS_QUEUE, (payload: any) => {
          try {
            const notification: INotification = {
              type: payload.type ?? 'SYSTEM',
              title: payload.title ?? payload.message,
              content: payload.content ?? payload.message,
              link: payload.link,
              timestamp: payload.timestamp ? new Date(payload.timestamp) : new Date(),
            };
            this.wsService.pushNotification(notification);
          } catch (err) {
            console.error('[ChatSocket] Notification parse error', err);
          }
        });
        this.notificationsSubscribed = true;
      } catch (err) {
        console.error('[ChatSocket] Failed to subscribe notifications', err);
      }
    }
  }
}
