import { Injectable, inject, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Client, IMessage } from '@stomp/stompjs';
import { BehaviorSubject, filter } from 'rxjs';
import { INotification } from '../model/notification.model';
import dayjs from 'dayjs';
import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import SockJS from 'sockjs-client';
import { ApplicationConfigService } from 'app/core/config/application-config.service';

export interface ChatMessage {
  user?: string;
  message: string;
  timestamp: Date;
  isFromAdmin: boolean;
  type?: 'message' | 'system' | 'SESSION_ENDED';
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  public notifications$;
  public unreadCount$;
  public chatMessage$;
  public connectionState$;

  private client?: Client;
  private currentUserId?: string;
  private notificationsSubject = new BehaviorSubject<INotification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private chatMessageSubject = new BehaviorSubject<ChatMessage | null>(null);
  private connectionSubject = new BehaviorSubject<boolean>(false);
  private ngZone = inject(NgZone);
  private accountService = inject(AccountService);
  private authServerProvider = inject(AuthServerProvider);
  private http = inject(HttpClient);
  private applicationConfigService = inject(ApplicationConfigService);
  private resourceUrl =
    this.applicationConfigService.getEndpointFor('api/notifications');

  constructor() {
    this.notifications$ = this.notificationsSubject.asObservable();
    this.unreadCount$ = this.unreadCountSubject.asObservable();
    this.chatMessage$ = this.chatMessageSubject
      .asObservable()
      .pipe(filter((msg): msg is ChatMessage => msg !== null));
    this.connectionState$ = this.connectionSubject.asObservable();
  }

  get isConnected(): boolean {
    return this.connectionSubject.value;
  }

  connect(userId: string): void {
    console.log(`[DEBUG] WebSocketService: connect() called for userId: ${userId}`);
    if (this.isConnected) {
      console.log('[DEBUG] WebSocketService: Already connected. Aborting.');
      return;
    }
    if (!userId) {
      console.log('[DEBUG] WebSocketService: No userId provided. Aborting.');
      return;
    }

    if (this.client) {
      console.log('[DEBUG] WebSocketService: Deactivating existing client.');
      try {
        this.client.deactivate();
      } catch (error) {
        // Silently ignore deactivation errors
      }
    }

    this.currentUserId = userId;
    const token = this.authServerProvider.getToken();
    if (!token) {
      console.warn('[DEBUG] WebSocketService: No token available for WebSocket connection. Aborting.');
      return;
    }

    const url = '/ws';
    console.log(`[DEBUG] WebSocketService: Attempting to connect to ${url}`);
    try {
      this.client = new Client({
        webSocketFactory() {
          try {
            const socket = new SockJS(url);
            console.log('[DEBUG] WebSocketService: SockJS socket created.');
            return socket;
          } catch (error) {
            console.error('[DEBUG] WebSocketService: SockJS socket creation failed.', error);
            throw error;
          }
        },
        connectHeaders: { 'X-Authorization': `Bearer ${token}` },
        reconnectDelay: 5000,
        heartbeatIncoming: 25000,
        heartbeatOutgoing: 25000,
        debug: (str) => {
          console.log(`[DEBUG] STOMP: ${str}`);
        },
        onConnect: () => {
          this.ngZone.run(() => {
            console.log('[DEBUG] WebSocketService: onConnect - Successfully connected.');
            this.connectionSubject.next(true);
            this.subscribeToTopics();
            this.loadNotificationsFromServer();
          });
        },
        onStompError: (frame) => {
          console.error('[DEBUG] WebSocketService: onStompError - Broker reported error: ' + frame.headers['message']);
          console.error('Additional details: ' + frame.body);
          this.ngZone.run(() => this.connectionSubject.next(false));
        },
        onWebSocketError: (event) => {
          console.error('[DEBUG] WebSocketService: onWebSocketError', event);
          this.ngZone.run(() => this.connectionSubject.next(false));
        },
        onWebSocketClose: () => {
          console.log('[DEBUG] WebSocketService: onWebSocketClose - connection closed.');
          this.ngZone.run(() => this.connectionSubject.next(false));
        },
        onDisconnect: () => {
          console.log('[DEBUG] WebSocketService: onDisconnect - disconnected.');
          this.ngZone.run(() => this.connectionSubject.next(false));
        },
      });
      console.log('[DEBUG] WebSocketService: Stomp client created. Activating...');
      this.client.activate();
    } catch (error) {
      console.error('[DEBUG] WebSocketService: Error creating WebSocket client:', error);
      this.connectionSubject.next(false);
    }
  }

  disconnect(): void {
    console.log('[DEBUG] WebSocketService: disconnect() called.');
    if (this.client) {
      try {
        if (this.client.connected) {
          console.log('[DEBUG] WebSocketService: Deactivating client.');
          this.client.deactivate();
        }
      } catch (error) {
        // Ignore error when disconnecting
      } finally {
        this.client = undefined;
      }
    }
    this.connectionSubject.next(false);
    this.currentUserId = undefined;
  }

  sendMessageToAdmin(message: string): void {
    if (!this.isConnected || !this.client) {
      console.warn('Cannot send message: WebSocket not connected');
      return;
    }
    try {
      this.client.publish({
        destination: '/app/topic/chat/admin',
        body: message,
      });
    } catch (error) {
      console.error('Error sending message to admin:', error);
    }
  }

  sendMessageToUser(userId: string, message: string): void {
    if (!this.isConnected || !this.client) {
      console.warn('Cannot send message: WebSocket not connected');
      return;
    }
    if (this.accountService.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_CSKH'])) {
      try {
        this.client.publish({
          destination: `/app/chat/${userId}`,
          body: message,
        });
      } catch (error) {
        console.error('Error sending message to user:', error);
      }
    }
  }

  markAsRead(id: number): void {
    console.log(`[DEBUG] WebSocketService: markAsRead() called for id: ${id}`);
    this.http.put(`${this.resourceUrl}/${id}/read`, {}).subscribe({
      next: () => {
        const notifications = this.notificationsSubject.value;
        const index = notifications.findIndex((n) => n.id === id);
        if (index !== -1) {
          notifications[index].read = true;
          this.notificationsSubject.next([...notifications]);
          this.updateUnreadCount();
        }
      },
    });
  }

  markAllAsRead(): void {
    console.log('[DEBUG] WebSocketService: markAllAsRead() called.');
    this.http.put(`${this.resourceUrl}/mark-all-read`, {}).subscribe({
      next: () => {
        const notifications = this.notificationsSubject.value;
        notifications.forEach((n) => (n.read = true));
        this.notificationsSubject.next([...notifications]);
        this.updateUnreadCount();
      },
    });
  }

  private loadNotificationsFromServer(): void {
    if (!this.currentUserId) return;
    console.log(`[DEBUG] WebSocketService: loadNotificationsFromServer() for userId: ${this.currentUserId}`);
    this.http.get<INotification[]>(this.resourceUrl).subscribe({
      next: (notifications) => {
        console.log(`[DEBUG] WebSocketService: Received ${notifications.length} notifications from server.`);
        const processed = notifications.map((n) => ({
          ...n,
          timestamp: n.timestamp ? dayjs(n.timestamp.toString()) : undefined,
        }));
        this.notificationsSubject.next(processed);
        this.updateUnreadCount();
      },
      error: (err) => {
        console.error('[DEBUG] WebSocketService: Error loading notifications from server.', err);
      }
    });
  }

  private subscribeToTopics(): void {
    if (!this.client || !this.currentUserId) return;
    console.log('[DEBUG] WebSocketService: Subscribing to topics...');

    try {
      // Subscribe to general notifications
      this.client.subscribe(
        `/user/queue/notifications`,
        (message: IMessage) => {
          console.log('[DEBUG] WebSocketService: Received message on /user/queue/notifications');
          this.handleNotificationMessage(message);
        },
      );

      this.client.subscribe(`/user/queue/chat`, (message: IMessage) => {
        console.log('[DEBUG] WebSocketService: Received message on /user/queue/chat');
        this.handleChatMessage(message, true);
      });

      if (this.accountService.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_CSKH'])) {
        console.log('[DEBUG] WebSocketService: Subscribing to admin chat topic.');
        this.client.subscribe('/topic/admin/chat', (message: IMessage) => {
          this.handleChatMessage(message, false);
        });
      }
      console.log('[DEBUG] WebSocketService: Subscriptions successful.');
    } catch (error) {
      console.error('[DEBUG] WebSocketService: Error subscribing to topics:', error);
    }
  }

  private handleNotificationMessage(message: IMessage): void {
    try {
      const notification = JSON.parse(message.body) as INotification;
      console.log('[DEBUG] WebSocketService: Parsed notification message:', notification);
      if (notification.timestamp) {
          notification.timestamp = dayjs(notification.timestamp.toString());
      }
      this.ngZone.run(() => {
        const currentNotifications = this.notificationsSubject.value;
        this.notificationsSubject.next([notification, ...currentNotifications]);
        this.updateUnreadCount();
      });
    } catch (error) {
      console.error('[DEBUG] WebSocketService: Error parsing notification message:', error);
    }
  }

  private handleChatMessage(message: IMessage, isFromAdmin: boolean): void {
    try {
      const parsed = JSON.parse(message.body);
      const chatMessage: ChatMessage = {
        user: parsed.user ?? undefined,
        message: parsed.message ?? parsed.content ?? parsed,
        timestamp: parsed.timestamp ? new Date(parsed.timestamp) : new Date(),
        isFromAdmin,
        type: parsed.type ?? 'message',
      };
      this.ngZone.run(() => this.chatMessageSubject.next(chatMessage));
    } catch (error) {
      const chatMessage: ChatMessage = {
        message: message.body,
        timestamp: new Date(),
        isFromAdmin,
        type: 'message',
      };
      this.ngZone.run(() => this.chatMessageSubject.next(chatMessage));
    }
  }

  private updateUnreadCount(): void {
    const unread = this.notificationsSubject.value.filter(
      (n) => !n.read,
    ).length;
    console.log(`[DEBUG] WebSocketService: Updating unread count to: ${unread}`);
    this.unreadCountSubject.next(unread);
  }
}
