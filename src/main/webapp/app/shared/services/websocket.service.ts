import { Injectable } from '@angular/core';
import SockJS from 'sockjs-client';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Subject, Observable } from 'rxjs';
import { WS_CONFIG } from 'app/core/websocket/websocket.config';
import { Dayjs } from 'dayjs';

// Minimal shared types expected by the codebase
export interface ChatMessage {
  conversationId?: number;
  senderType?: 'USER' | 'ADMIN' | 'CSKH' | 'SYSTEM';
  senderIdentifier?: string;
  content?: string;
  message?: string; // alias used in UI
  isFromAdmin?: boolean;
  timestamp?: string | Date | undefined;
  type?: 'MESSAGE' | 'SESSION_ENDED' | 'system';
}

export interface INotification {
  id?: number | string;
  type?: string; // optional to remain compatible with model INotification
  title?: string;
  content?: string;
  message?: string;
  link?: string;
  timestamp?: string | Date | Dayjs;
  read?: boolean;
  orderId?: number | string;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  // Public observables (declared before private fields to satisfy ESLint member-ordering)
  public connectionState$: Observable<boolean>;
  public notifications$: Observable<INotification[]>;
  public unreadCount$: Observable<number>;
  public chatMessage$: Subject<ChatMessage>;

  // Public convenience boolean used directly in templates in the project
  public isConnected = false;

  // Private fields
  private client?: Client;
  private connectionStateSubject = new BehaviorSubject<boolean>(false);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private notificationsSubject = new BehaviorSubject<INotification[]>([]);
  private subscriptions: Map<string, StompSubscription> = new Map<string, StompSubscription>();

  constructor() {
    this.connectionState$ = this.connectionStateSubject.asObservable();
    this.notifications$ = this.notificationsSubject.asObservable();
    this.chatMessage$ = new Subject<ChatMessage>();
    this.unreadCount$ = this.unreadCountSubject.asObservable();

    // keep boolean in sync with subject
    this.connectionState$.subscribe(v => (this.isConnected = v));
  }

  /**
   * Connect to backend STOMP endpoint.
   * Flexible signature to accept either:
   *  - connect(token)  // common usage in many components
   *  - connect(endpoint, token)
   */
  public connect(arg1: string = WS_CONFIG.BASE_URL, arg2?: string): void {
    let endpoint = WS_CONFIG.BASE_URL;
    let token: string | undefined;

    // Detect if arg1 looks like a JWT (has at least two dots) -> treat as token
    const looksLikeToken = (s?: string): boolean => (s?.match(/\./g) ?? []).length >= 2;

    if (!arg1) {
      endpoint = WS_CONFIG.BASE_URL;
      token = arg2;
    } else if (looksLikeToken(arg1) && !arg1.startsWith('/')) {
      // called as connect(token)
      token = arg1;
    } else if (arg1.startsWith('/') || arg1.startsWith('http') || arg1.includes('?')) {
      // called as connect(endpoint) or connect(endpoint, token)
      endpoint = arg1;
      token = arg2;
    } else if (arg2 && looksLikeToken(arg2)) {
      // called as connect(endpoint, token)
      endpoint = arg1;
      token = arg2;
    } else {
      // Ambiguous: treat arg1 as token fallback
      token = arg1;
    }

    // If already connected, skip
    if (this.client && (this.client.active || (this.client as any).connected)) {
      return;
    }

    // build wsUrl from endpoint (if endpoint starts with / use host/port)
    let wsUrl = endpoint;
    if (endpoint.startsWith('/')) {
      const httpProtocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
      const hostname = window.location.hostname || 'localhost';
      const backendPort = '8080';
      wsUrl = `${httpProtocol}//${hostname}:${backendPort}${endpoint}`;
    }

    // If endpoint already contains query params (e.g., ?guest=...), do not append token param
    const fullUrl = token ? `${wsUrl}${wsUrl.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}` : wsUrl;
    console.warn('[WebSocketService] Connecting to:', fullUrl.replace(/token=([^&]+)/, 'token=***'));

    this.client = new Client({
      webSocketFactory: () => new SockJS(fullUrl),
      reconnectDelay: WS_CONFIG.RECONNECT_DELAY,
      heartbeatIncoming: WS_CONFIG.HEARTBEAT_INCOMING,
      heartbeatOutgoing: WS_CONFIG.HEARTBEAT_OUTGOING,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    });

    const client = this.client;

    client.onConnect = (): void => {
      console.warn('[WebSocketService] STOMP CONNECTED');
      this.connectionStateSubject.next(true);
    };

    client.onWebSocketClose = (): void => {
      console.warn('[WebSocketService] WebSocket closed');
      this.connectionStateSubject.next(false);
    };

    client.onStompError = (frame?: any): void => {
      console.error('[WebSocketService] STOMP error', frame?.headers?.message ?? frame);
      this.connectionStateSubject.next(false);
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
      if (this.client) {
        try {
          this.client.deactivate();
        } catch (e) {
          /* noop */
        }
      }
    } finally {
      this.connectionStateSubject.next(false);
    }
  }

  public subscribe(destination: string, callback: (msg: any) => void): void {
    if (!this.client) {
      console.warn('[WebSocketService] subscribe called before connect', destination);
      return;
    }

    // avoid double subscription
    if (this.subscriptions.has(destination)) {
      return;
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
  }

  public unsubscribe(destination: string): void {
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
    if (!this.client) {
      console.warn('[WebSocketService] publish called before connect', destination);
      return;
    }
    try {
      this.client.publish({ destination, body: JSON.stringify(body) });
    } catch (e) {
      console.warn('[WebSocketService] publish error', e);
    }
  }

  // Convenience methods used across the codebase
  public sendMessage(payload: any): void {
    this.publish(WS_CONFIG.BROKER.CHAT_SEND, payload);
  }

  public sendReplyAsCskh(conversationId: number, content: string): void {
    this.publish(WS_CONFIG.BROKER.CHAT_REPLY, { conversationId, content });
  }

  public openChat(conversationId: number): void {
    // Subscribe to the standardized conversation topic: /topic/chat/conversations/{id}
    const dest = `${WS_CONFIG.BROKER.CHAT_CONVERSATIONS_PREFIX}/${conversationId}`;
    this.subscribe(dest, (msg: ChatMessage) => {
      this.chatMessage$.next(msg);
    });

    // Also notify server that we opened (optional)
    this.publish('/app/chat.open', { conversationId });
  }

  public closeChat(conversationId: number): void {
    this.publish('/app/chat.close', { conversationId });
    this.unsubscribe(`${WS_CONFIG.BROKER.CHAT_CONVERSATIONS_PREFIX}/${conversationId}`);
  }

  // push notification into subject
  public pushNotification(notification: INotification): void {
    try {
      // prepend to notifications list
      const curr = this.notificationsSubject.getValue();
      this.notificationsSubject.next([notification, ...curr]);
      // increment unread counter
      this.unreadCountSubject.next(this.unreadCountSubject.getValue() + 1);
    } catch (e) {
      console.warn('[WebSocketService] pushNotification failed', e);
    }
  }
}
