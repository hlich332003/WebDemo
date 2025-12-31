import { Injectable, inject, OnDestroy } from '@angular/core';
import { WebSocketService, ChatMessage } from 'app/shared/services/websocket.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { BehaviorSubject, Subject, Observable, Subscription } from 'rxjs';
import { WS_CONFIG } from './websocket.config';
import { INotification } from 'app/shared/model/notification.model';

@Injectable({ providedIn: 'root' })
export class ChatSocket implements OnDestroy {
  public readonly connectionState$: Observable<boolean>;

  private wsService = inject(WebSocketService);
  private storage = inject(StateStorageService);

  private connectionSub?: Subscription;
  private notificationsSubscribed = false;

  private connected$ = new BehaviorSubject<boolean>(false);
  private message$ = new Subject<ChatMessage>();

  private destroy$ = new Subject<never>();

  constructor() {
    this.connectionState$ = this.connected$.asObservable();

    this.wsService.onConnected().subscribe(() => {
      console.warn('[ChatSocket] WebSocket is connected, ensuring subscriptions.');
      this.ensureSubscriptions();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next(undefined as never);
    this.destroy$.complete();
    this.disconnect();
  }

  connect(endpoint?: string, token?: string): void {
    if (this.wsService.isConnected) {
      return;
    }

    const providedToken = token ?? this.storage.getAuthenticationToken();
    const providedEndpoint = endpoint ?? '/websocket';

    // FIX: Always pass endpoint as first param, token as second param
    this.wsService.connect(providedEndpoint, providedToken ?? undefined);
  }

  openChat(conversationId: number): void {
    const topic = `/topic/chat.${conversationId}`;

    console.warn(`[ChatSocket] 📡 Subscribing to ${topic}`);

    // ✅ FIX: Subscribe returns void, don't try to assign it
    this.wsService.subscribe(topic, (m: any) => {
      try {
        console.warn(`[ChatSocket] 📨 Received message on ${topic}:`, m);
        const msg: ChatMessage = {
          content: m.content ?? m.message,
          senderType: m.senderType,
          timestamp: m.createdAt ? new Date(m.createdAt) : new Date(),
          type: m.type,
          conversationId: m.conversationId,
          senderIdentifier: m.senderIdentifier,
          message: m.message,
          isFromAdmin: m.senderType === 'CSKH' || m.senderType === 'ADMIN' || m.isFromAdmin === true,
        };
        this.message$.next(msg);
      } catch (e) {
        console.error('[ChatSocket] parse message error', e);
      }
    });
  }

  sendMessage(payload: { conversationId?: number | undefined; content: string }): void {
    console.warn('[ChatSocket] 📤 Sending message:', payload);
    // FIX: Send to /app/chat.send as expected by ChatController
    this.wsService.publish('/app/chat.send', payload);
  }

  disconnect(): void {
    try {
      if (this.notificationsSubscribed) {
        this.wsService.unsubscribe(WS_CONFIG.BROKER.NOTIFICATIONS_QUEUE);
        this.notificationsSubscribed = false;
      }
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

  private ensureSubscriptions(): void {
    this.connectionSub ??= this.wsService.connectionState$.subscribe(v => {
      this.connected$.next(v);
    });

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
