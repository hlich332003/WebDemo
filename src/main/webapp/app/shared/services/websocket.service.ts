import { Injectable } from '@angular/core';
import SockJS from 'sockjs-client';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Subject, Observable } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { WS_CONFIG } from 'app/core/websocket/websocket.config';
import { INotification } from 'app/shared/model/notification.model';

export interface ChatMessage {
  conversationId?: number;
  senderType?: 'USER' | 'ADMIN' | 'CSKH' | 'SYSTEM' | 'GUEST';
  senderIdentifier?: string;
  content?: string;
  message?: string;
  isFromAdmin?: boolean;
  timestamp?: string | Date | undefined;
  type?: 'MESSAGE' | 'SESSION_ENDED' | 'system';
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  public connectionState$: Observable<boolean>;
  public notifications$: Observable<INotification[]>;
  public unreadCount$: Observable<number>;
  public chatMessage$: Subject<ChatMessage>;

  public isConnected = false;
  private isConnecting = false;

  private client?: Client;
  private connectionStateSubject = new BehaviorSubject<boolean>(false);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private notificationsSubject = new BehaviorSubject<INotification[]>([]);
  private subscriptions: Map<string, StompSubscription> = new Map<string, StompSubscription>();

  private pendingSubscriptions: Map<string, (msg: any) => void> = new Map<string, (msg: any) => void>();

  constructor() {
    this.connectionState$ = this.connectionStateSubject.asObservable();
    this.notifications$ = this.notificationsSubject.asObservable();
    this.chatMessage$ = new Subject<ChatMessage>();
    this.unreadCount$ = this.unreadCountSubject.asObservable();

    this.connectionState$.subscribe(v => (this.isConnected = v));
  }

  public onConnected(): Observable<boolean> {
    return this.connectionState$.pipe(
      filter(v => v),
      take(1),
    );
  }

  public connect(arg1: string = WS_CONFIG.BASE_URL, arg2?: string): void {
    if (this.isConnected || this.isConnecting || this.client?.active) {
      console.warn('[WebSocketService] Already connected or connecting. Skipping.');
      return;
    }

    let endpoint = WS_CONFIG.BASE_URL;
    let token: string | undefined;

    // Helper to check if a string looks like a JWT token
    const looksLikeToken = (s?: string): boolean => {
      if (!s) return false;
      // JWT has 2 dots (3 parts)
      const parts = s.split('.');
      return parts.length === 3 && parts.every(p => p.length > 0);
    };

    // Parse arguments
    if (!arg1) {
      // No args: use default endpoint, no token
      endpoint = WS_CONFIG.BASE_URL;
      token = arg2;
    } else if (looksLikeToken(arg1) && !arg1.startsWith('/') && !arg1.startsWith('http')) {
      // First arg looks like token
      token = arg1;
      endpoint = WS_CONFIG.BASE_URL;
    } else if (arg1.startsWith('/') || arg1.startsWith('http') || arg1.includes('?')) {
      // First arg is endpoint
      endpoint = arg1;
      token = arg2;
    } else {
      // Default: arg1 is endpoint, arg2 is token
      endpoint = arg1 || WS_CONFIG.BASE_URL;
      token = arg2;
    }

    // Build full WebSocket URL
    let wsUrl = endpoint;
    if (endpoint.startsWith('/')) {
      const httpProtocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
      const hostname = window.location.hostname || 'localhost';
      const backendPort = '8080';
      wsUrl = `${httpProtocol}//${hostname}:${backendPort}${endpoint}`;
    }

    let fullUrl = wsUrl;
    if (token) {
      fullUrl = `${wsUrl}${wsUrl.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}`;
    } else {
      const guestId = localStorage.getItem('chat_guest_id');
      if (guestId) {
        fullUrl = `${wsUrl}${wsUrl.includes('?') ? '&' : '?'}guestId=${encodeURIComponent(guestId)}`;
      }
    }

    console.warn('[WebSocketService] 🔌 Connecting to:', fullUrl.replace(/token=([^&]+)/, 'token=***'));

    this.isConnecting = true;

    this.client = new Client({
      webSocketFactory: () => new SockJS(fullUrl),
      reconnectDelay: WS_CONFIG.RECONNECT_DELAY,
      heartbeatIncoming: WS_CONFIG.HEARTBEAT_INCOMING,
      heartbeatOutgoing: WS_CONFIG.HEARTBEAT_OUTGOING,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug(str) {
        if (str.includes('ERROR') || str.includes('error')) {
          console.error('[STOMP DEBUG]', str);
        }
      },
    });

    const client = this.client;

    client.onConnect = (): void => {
      console.warn('[WebSocketService] ✅ STOMP CONNECTED successfully');
      this.isConnecting = false;
      this.connectionStateSubject.next(true);

      try {
        this.pendingSubscriptions.forEach((cb, dest) => {
          if (!this.subscriptions.has(dest)) {
            console.warn('[WebSocketService] Subscribing to pending:', dest);
            const sub = client.subscribe(dest, (message: IMessage) => {
              try {
                const body = message.body ? JSON.parse(message.body) : null;
                cb(body);
              } catch (e) {
                console.warn('[WebSocketService] failed parse message', e);
              }
            });
            this.subscriptions.set(dest, sub);
          }
        });
        this.pendingSubscriptions.clear();
      } catch (e) {
        console.warn('[WebSocketService] failed to flush pending subscriptions', e);
      }
    };

    client.onWebSocketClose = (event?: any): void => {
      console.warn('[WebSocketService] ⚠️ WebSocket closed', event?.code, event?.reason);
      this.isConnecting = false;
      this.connectionStateSubject.next(false);
    };

    client.onStompError = (frame?: any): void => {
      console.error('[WebSocketService] ❌ STOMP error', frame?.headers?.message ?? frame);
      this.isConnecting = false;
      this.connectionStateSubject.next(false);
    };

    client.onWebSocketError = (error?: any): void => {
      console.error('[WebSocketService] ❌ WebSocket error', error);
    };

    client.activate();
  }

  public disconnect(): void {
    try {
      this.subscriptions.forEach(sub => {
        try {
          sub.unsubscribe();
        } catch (e) {
          /* noop */
        }
      });
      this.subscriptions.clear();
      this.pendingSubscriptions.clear();
      if (this.client) {
        try {
          this.client.deactivate();
        } catch (e) {
          /* noop */
        }
      }
    } finally {
      this.isConnecting = false;
      this.connectionStateSubject.next(false);
    }
  }

  public subscribe(destination: string, callback: (msg: any) => void): StompSubscription | undefined {
    if (!this.client?.active) {
      this.pendingSubscriptions.set(destination, callback);
      return undefined;
    }

    if (this.subscriptions.has(destination)) {
      return this.subscriptions.get(destination);
    }

    const sub = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const body = message.body ? JSON.parse(message.body) : null;
        callback(body);
      } catch (e) {
        console.warn('[WebSocketService] failed parse message', e);
      }
    });

    this.subscriptions.set(destination, sub);
    return sub;
  }

  public unsubscribe(destination: string): void {
    if (this.pendingSubscriptions.has(destination)) {
      this.pendingSubscriptions.delete(destination);
    }

    const sub = this.subscriptions.get(destination);
    if (sub) {
      try {
        sub.unsubscribe();
      } catch (e) {
        /* noop */
      }
      this.subscriptions.delete(destination);
    }
  }

  public publish(destination: string, body: any): void {
    if (!this.client?.active || !this.isConnected) {
      console.warn(
        `[WebSocketService] ❌ Cannot publish to ${destination}: Not connected (active=${this.client?.active}, isConnected=${this.isConnected})`,
      );
      return;
    }
    try {
      console.warn(`[WebSocketService] 📤 Publishing to ${destination}:`, body);
      this.client.publish({ destination, body: JSON.stringify(body) });
      console.warn(`[WebSocketService] ✅ Published successfully to ${destination}`);
    } catch (e) {
      console.error('[WebSocketService] ❌ Publish error:', e);
    }
  }

  public sendMessage(payload: any): void {
    this.publish(WS_CONFIG.BROKER.CHAT_SEND, payload);
  }

  public sendReplyAsCskh(conversationId: number, content: string): void {
    this.publish(WS_CONFIG.BROKER.CHAT_REPLY, { conversationId, content });
  }

  public openChat(conversationId: number): void {
    const dest = `${WS_CONFIG.BROKER.CHAT_CONVERSATIONS_PREFIX}.${conversationId}`;
    this.subscribe(dest, (msg: ChatMessage) => {
      this.chatMessage$.next(msg);
    });
    if (this.isConnected) {
      // this.publish('/app/chat.open', { conversationId }); // Removed as it's not used in backend
    }
  }

  public closeChat(conversationId: number): void {
    // this.publish('/app/chat.close', { conversationId }); // Removed as it's not used in backend
    this.unsubscribe(`${WS_CONFIG.BROKER.CHAT_CONVERSATIONS_PREFIX}.${conversationId}`);
  }

  public pushNotification(notification: INotification): void {
    try {
      const curr = this.notificationsSubject.getValue();
      this.notificationsSubject.next([notification, ...curr]);
      this.unreadCountSubject.next(this.unreadCountSubject.getValue() + 1);
    } catch (e) {
      console.warn('[WebSocketService] pushNotification failed', e);
    }
  }
}
