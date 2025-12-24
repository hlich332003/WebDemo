import { Injectable, OnDestroy, inject } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { INotification } from '../model/notification.model';

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
export class WebSocketService implements OnDestroy {
  // Public streams (Declared first)
  public connectionState$;
  public chatMessage$;
  public notifications$;
  public unreadCount$;
  public cskhChatStream_;

  // Private fields
  private client: Client | null = null;
  private connected$ = new BehaviorSubject<boolean>(false);
  private chatMessageSubject = new Subject<ChatMessage>();
  private notificationsSubject = new BehaviorSubject<INotification[]>([]);
  private config = inject(ApplicationConfigService);

  // Prevent multiple activate attempts
  private activating = false;

  constructor() {
    // Initialize public streams from private subjects
    this.connectionState$ = this.connected$.asObservable();
    this.chatMessage$ = this.chatMessageSubject.asObservable();
    this.notifications$ = this.notificationsSubject.asObservable();
    this.unreadCount$ = this.notifications$.pipe(map(list => list.filter(n => !n.read).length));
    this.cskhChatStream_ = this.chatMessageSubject.asObservable();
  }

  /**
   * Kết nối WebSocket với Token cụ thể
   * subscribeUserQueue: nếu true thì subscribe vào /user/queue/chat (dành cho user)
   * Nếu false: chỉ subscribe notifications (dành cho admin/cskh hoặc user không cần chat)
   */
  connect(token: string, subscribeUserQueue = false): void {
    console.warn('🚀 WS connect called, token:', token, 'subscribeUserQueue:', subscribeUserQueue);

    // Use connected$ value as source of truth
    if (this.connected$.value) {
      console.warn('WebSocket already active, skipping connect');
      return; // Already connected
    }

    if (this.activating) {
      console.warn('WebSocket activation already in progress');
      return;
    }

    if (!token) {
      console.warn('Cannot connect to WebSocket: Token is empty');
      return;
    }

    const endpoint = this.config.getEndpointFor('/websocket');
    let wsUrl: string;
    try {
      const isAbsolute = endpoint.startsWith('http://') || endpoint.startsWith('https://');
      if (isAbsolute) {
        // If the endpoint from config is absolute (includes host), use it but ensure it's HTTP/HTTPS
        const asUrl = new URL(endpoint);
        const httpScheme = asUrl.protocol === 'https:' ? 'https:' : 'http:';
        // SockJS expects an HTTP(S) URL (not ws://). Keep query param for token below.
        wsUrl = `${httpScheme}//${asUrl.host}${asUrl.pathname}?access_token=${token}`;
      } else if (endpoint.startsWith('/')) {
        // If endpoint is relative, always point to backend host: default to port 8080
        // Use HTTP/HTTPS scheme here (SockJS requires http/https) to avoid the browser
        // attempting to connect back to BrowserSync (running on 9001) which would
        // intercept/kill the socket. This forces SockJS to connect to backend:8080.
        const httpProtocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
        const hostname = window.location.hostname || 'localhost';
        const backendPort = '8080';
        wsUrl = `${httpProtocol}//${hostname}:${backendPort}${endpoint}?access_token=${token}`;
      } else {
        // Fallback: construct using same host but explicit port 8080 and HTTP scheme
        const httpProtocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
        const hostname = window.location.hostname || 'localhost';
        const backendPort = '8080';
        // remove any leading slash from endpoint when concatenating
        wsUrl = `${httpProtocol}//${hostname}:${backendPort}/${endpoint.replace(/^\//, '')}?access_token=${token}`;
      }
    } catch (e) {
      // On any error, fallback to explicit backend host (HTTP) to avoid using BrowserSync proxy
      const httpProtocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
      const hostname = window.location.hostname || 'localhost';
      const backendPort = '8080';
      wsUrl = `${httpProtocol}//${hostname}:${backendPort}/websocket?access_token=${token}`;
    }
    console.warn('🌐 WS URL:', wsUrl);

    try {
      this.client = new Client({
        // SockJS expects an HTTP(S) endpoint, not ws://. Use the HTTP URL built above.
        webSocketFactory: () => new SockJS(wsUrl),
        debug: (str: string) => console.warn('[STOMP]', str),
        reconnectDelay: 0,
        heartbeatIncoming: 25000,
        heartbeatOutgoing: 25000,
        connectHeaders: {
          Authorization: `Bearer ${token}`,
          access_token: token,
        } as any,
      });

      this.activating = true;

      this.client.onConnect = () => {
        console.warn('✅ WebSocket Connected!');
        this.activating = false;
        this.connected$.next(true);

        // Notifications always subscribed
        try {
          this.subscribeNotifications();
        } catch (e) {
          console.error('Failed to subscribe notifications', e);
        }

        if (subscribeUserQueue) {
          try {
            this.subscribeUserChat();
          } catch (e) {
            console.error('Failed to subscribe user chat', e);
          }
        }
      };

      this.client.onStompError = frame => {
        console.error('❌ Broker reported error:', frame);
        // mark disconnected but do not throw
        this.connected$.next(false);
        this.activating = false;
      };

      this.client.onWebSocketClose = (evt?: any) => {
        console.warn('❌ WebSocket Closed', evt?.reason ?? evt);
        this.connected$.next(false);
        this.activating = false;
        // keep client object so we know there was a connection attempt
      };

      this.client.activate();
    } catch (e) {
      console.error('WebSocket activate failed', e);
      this.activating = false;
      // Ensure we don't leave inconsistent state
      this.client = null;
      this.connected$.next(false);
    }
  }

  disconnect(): void {
    if (this.client) {
      try {
        this.client.deactivate();
      } catch (e) {
        console.warn('Error during WebSocket deactivate', e);
      }
      this.client = null;
    }
    this.activating = false;
    this.connected$.next(false);
  }

  sendMessage(content: string): void {
    if (!this.connected$.value || !this.client) return;

    const payload = {
      content,
      timestamp: new Date().toISOString(),
    };

    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(payload),
    });
  }

  sendReplyAsCskh(conversationId: number, content: string): void {
    if (!this.connected$.value || !this.client) return;

    this.client.publish({
      destination: '/app/chat.reply',
      body: JSON.stringify({ conversationId, content }),
    });
  }

  // Stubs
  openChat(_conversationId?: number): void {
    // Stub
  }

  closeChat(_conversationId?: number): void {
    // Stub
  }

  get isConnected(): boolean {
    return this.connected$.value;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  // ===== PRIVATE HELPERS =====

  private subscribeUserChat(): void {
    try {
      this.client?.subscribe('/user/queue/chat', (message: IMessage) => {
        try {
          if (message.body) {
            const payload = JSON.parse(message.body);
            const chatMsg: ChatMessage = {
              content: payload.content,
              message: payload.content,
              senderType: payload.senderType,
              timestamp: new Date(payload.timestamp ?? payload.createdAt),
              type: payload.type ?? 'MESSAGE',
              isFromAdmin: payload.senderType !== 'USER',
            };
            this.chatMessageSubject.next(chatMsg);
          }
        } catch (err) {
          console.error('Failed parsing user chat message', err, message.body);
        }
      });
    } catch (err) {
      console.error('subscribeUserChat error', err);
    }
  }

  private subscribeNotifications(): void {
    try {
      this.client?.subscribe('/user/queue/notifications', (message: IMessage) => {
        try {
          if (message.body) {
            const notification: INotification = JSON.parse(message.body);
            const current = this.notificationsSubject.value;
            this.notificationsSubject.next([notification, ...current]);
          }
        } catch (err) {
          console.error('Failed parsing notification message', err, message.body);
        }
      });
    } catch (err) {
      console.error('subscribeNotifications error', err);
    }
  }
}
