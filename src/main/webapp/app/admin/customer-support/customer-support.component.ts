import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef, NgZone } from '@angular/core';
import { CommonModule, SlicePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime } from 'rxjs/operators';
import { WebSocketService, ChatMessage } from 'app/shared/services/websocket.service';
import { WS_CONFIG } from 'app/core/websocket/websocket.config';
import { AccountService } from 'app/core/auth/account.service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Account } from 'app/core/auth/account.model';

interface Conversation {
  id: number;
  userIdentifier: string;
  status: string;
  lastMessageAt: string;
  unreadCount: number;
}

interface Message {
  id?: number;
  conversationId: number;
  senderType: 'USER' | 'ADMIN' | 'SYSTEM' | 'CSKH' | 'GUEST';
  senderIdentifier: string;
  content: string;
  createdAt: string;
  isFromAdmin?: boolean;
}

type ConversationType = 'CHAT' | 'TICKET';

@Component({
  selector: 'jhi-customer-support',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule, SlicePipe],
  templateUrl: './customer-support.component.html',
  styleUrls: ['./customer-support.component.scss'],
})
export class CustomerSupportComponent implements OnInit, OnDestroy {
  isConnected = false;
  adminUser: Account | null = null;
  conversations: Conversation[] = [];
  selectedConversation: Conversation | null = null;
  messages: Message[] = [];
  newMessage = '';
  isLoadingMessages = false;
  activeTab: ConversationType = 'CHAT';

  private webSocketService = inject(WebSocketService);
  private accountService = inject(AccountService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private ngZone = inject(NgZone);
  private destroy$ = new Subject<never>();

  @ViewChild('chatMessagesContainer')
  private chatMessagesContainer?: ElementRef;

  private currentConversationSub?: string;
  private scrollPending = false;
  private userScrolled = false;
  private lastScrollHeight = 0;

  ngOnInit(): void {
    this.accountService.getAuthenticationState().subscribe(account => {
      this.adminUser = account;
    });

    if (!this.accountService.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_CSKH'])) {
      return;
    }

    this.loadConversations();

    this.webSocketService.connectionState$.pipe(takeUntil(this.destroy$)).subscribe((isConnected: boolean) => {
      this.isConnected = isConnected;
      console.warn('[CustomerSupport] WS connected:', isConnected);

      // Subscribe to admin topics for new conversations and updates
      if (isConnected) {
        // Listen for new conversations/tickets - subscribe to broadcast topic
        this.webSocketService.subscribe('/topic/admin/chat-notifications', (msg: any) => {
          console.warn('[CustomerSupport] 📬 Received new chat notification:', msg);
          this.loadConversations(); // Reload list when new conversation
          this.cdr.detectChanges();
        });

        // Listen for user-specific notifications
        this.webSocketService.subscribe('/user/queue/chat', (msg: any) => {
          console.warn('[CustomerSupport] 📬 Received admin chat notification:', msg);
          this.loadConversations();
          this.cdr.detectChanges();
        });

        // Listen for general notifications (new tickets, etc.)
        this.webSocketService.subscribe('/user/queue/notifications', (msg: any) => {
          console.warn('[CustomerSupport] 🔔 Received notification:', msg);
          this.loadConversations();
          this.cdr.detectChanges();
        });
      }

      try {
        this.cdr.detectChanges();
      } catch (e) {
        // ignore if change detection not available
      }
    });

    this.webSocketService.chatMessage$.pipe(takeUntil(this.destroy$)).subscribe((msg: ChatMessage) => {
      this.handleIncomingMessage(msg);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next(undefined as never);
    this.destroy$.complete();
    if (this.currentConversationSub) {
      this.webSocketService.unsubscribe(this.currentConversationSub);
    }
  }

  onUserScroll(): void {
    if (!this.chatMessagesContainer) return;

    const container = this.chatMessagesContainer.nativeElement;
    const isAtBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 50;

    // User has scrolled up
    this.userScrolled = !isAtBottom;
  }

  setActiveTab(tab: ConversationType): void {
    this.activeTab = tab;
    this.selectedConversation = null;
    this.messages = [];
    this.userScrolled = false;
    this.loadConversations();
  }

  loadConversations(): void {
    const params = new HttpParams().set('type', this.activeTab);
    this.http.get<Conversation[]>('/api/chat/conversations', { params }).subscribe(convos => {
      this.conversations = convos;
      this.sortConversations();
    });
  }

  selectConversation(conversation: Conversation): void {
    // Unsubscribe from previous conversation topic
    if (this.currentConversationSub) {
      this.webSocketService.unsubscribe(this.currentConversationSub);
      this.currentConversationSub = undefined;
    }

    this.selectedConversation = conversation;
    // Reset unread count locally immediately
    conversation.unreadCount = 0;

    // Clear messages array before loading new conversation
    this.messages = [];
    this.isLoadingMessages = true;
    this.userScrolled = false;

    // Subscribe to conversation-specific topic FIRST before loading messages
    const topic = `${WS_CONFIG.BROKER.CHAT_CONVERSATIONS_PREFIX}.${conversation.id}`;
    this.currentConversationSub = topic;

    console.warn(`[CustomerSupport] 🔔 Subscribing to ${topic}`);
    this.webSocketService.subscribe(topic, (msg: any) => {
      console.warn('[CustomerSupport] 📩 Received message on topic:', msg);
      // Process incoming message through chatMessage$ stream
      this.webSocketService.chatMessage$.next(msg);
    });

    // Load existing messages from database (REST API - NOT WebSocket)
    this.http.get<Message[]>(`/api/support/tickets/${conversation.id}/messages`).subscribe({
      next: messages => {
        console.warn(`[CustomerSupport] 📜 Loaded ${messages.length} messages for conversation #${conversation.id}`);

        // Map and sort messages by createdAt (oldest first for display)
        this.messages = messages
          .map(m => ({
            ...m,
            content: m.content ?? (m as any).message ?? '', // Ensure content field exists
          }))
          .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

        console.warn('[CustomerSupport] 📊 Sorted messages count:', this.messages.length);
        this.isLoadingMessages = false;
        this.cdr.detectChanges();
        this.scheduleScrollToBottom(true);
        // Mark as read on server
        this.http.put(`/api/support/tickets/${conversation.id}/messages/mark-as-read`, {}).subscribe();
      },
      error: err => {
        console.error('[CustomerSupport] ❌ Failed to load messages:', err);
        this.isLoadingMessages = false;
      },
    });
  }

  sendMessage(): void {
    if (this.newMessage.trim() === '' || !this.selectedConversation) {
      return;
    }

    const conversationId = this.selectedConversation.id;
    const content = this.newMessage.trim();
    const adminIdentifier = this.adminUser?.email ?? 'Admin';

    console.warn('[CustomerSupport] 📤 Sending message:', {
      conversationId,
      content: content.substring(0, 50),
      sender: adminIdentifier,
    });

    // Optimistic update: Add message immediately to UI with temp flag
    const tempMsg: any = {
      conversationId,
      senderType: 'ADMIN',
      senderIdentifier: adminIdentifier,
      content,
      createdAt: new Date().toISOString(),
      isFromAdmin: true,
      _temp: true, // Mark as temporary
    };
    this.messages.push(tempMsg);
    this.userScrolled = false; // Reset user scroll when sending
    this.scheduleScrollToBottom(true);

    // Send via WebSocket
    this.webSocketService.sendMessage({
      conversationId,
      content,
    });

    this.newMessage = '';
  }

  closeConversation(conversationId: number): void {
    if (confirm('Bạn có chắc muốn đóng cuộc trò chuyện này không?')) {
      this.http.post(`/api/chat/conversations/${conversationId}/close`, {}).subscribe({
        next: () => {
          // Update local state
          if (this.selectedConversation?.id === conversationId && this.selectedConversation) {
            this.selectedConversation.status = 'CLOSED';
          }
          const convo = this.conversations.find(c => c.id === conversationId);
          if (convo) {
            convo.status = 'CLOSED';
          }
          this.sortConversations();
          this.cdr.detectChanges();
        },
        error() {
          alert('Không thể đóng phiên chat. Vui lòng thử lại.');
        },
      });
    }
  }

  trackByConversationId(index: number, item: Conversation): number {
    return item.id;
  }

  trackByMessageId(index: number, item: Message): number | undefined {
    return item.id;
  }

  private handleIncomingMessage(msg: ChatMessage): void {
    console.warn('[CustomerSupport] 📨 Handling incoming message:', {
      conversationId: msg.conversationId,
      type: msg.type,
      isFromAdmin: msg.isFromAdmin,
      content: msg.content?.substring(0, 50),
      unreadCount: (msg as any).unreadCount,
    });

    // Handle SESSION_ENDED type
    if (msg.type === 'SESSION_ENDED') {
      console.warn('[CustomerSupport] 🔒 Session ended for conversation:', msg.conversationId);

      // Update conversation list
      const conversation = this.conversations.find(c => c.id === msg.conversationId);
      if (conversation) {
        conversation.status = 'CLOSED';
        this.sortConversations();
      }

      // Update selected conversation if it's the one being closed
      if (this.selectedConversation && this.selectedConversation.id === msg.conversationId) {
        this.selectedConversation.status = 'CLOSED';

        // Add system message to chat
        this.messages.push({
          conversationId: msg.conversationId ?? 0,
          senderIdentifier: 'SYSTEM',
          senderType: 'SYSTEM',
          content: msg.message ?? 'Phiên chat đã kết thúc',
          createdAt: new Date().toISOString(),
          isFromAdmin: true,
          id: undefined,
        });

        // Reset scroll state to allow viewing history
        this.userScrolled = false;
        this.scheduleScrollToBottom(true);
      }

      this.cdr.detectChanges();
      return;
    }

    // Find conversation
    const conversation = this.conversations.find(c => c.id === msg.conversationId);

    if (conversation) {
      const ts = msg.timestamp ? (typeof msg.timestamp === 'string' ? new Date(msg.timestamp) : msg.timestamp) : new Date();
      conversation.lastMessageAt = ts.toISOString();

      // ✅ Update unreadCount from WebSocket payload (if available)
      if ((msg as any).unreadCount !== undefined) {
        conversation.unreadCount = (msg as any).unreadCount;
        console.warn(`[CustomerSupport] 🔢 Updated unreadCount for conversation #${msg.conversationId}: ${conversation.unreadCount}`);
      }

      // ✅ Update lastMessageAt from WebSocket payload (if available)
      if ((msg as any).lastMessageAt) {
        conversation.lastMessageAt = (msg as any).lastMessageAt;
      }

      if (this.selectedConversation?.id === msg.conversationId) {
        // Remove temp messages from optimistic update
        const tempIndex = this.messages.findIndex(m => (m as any)._temp && m.content === (msg.content ?? msg.message ?? ''));
        if (tempIndex >= 0) {
          console.warn('[CustomerSupport] 🔄 Removing temp optimistic message');
          this.messages.splice(tempIndex, 1);
        }

        // Check for duplicates
        const msgContent = msg.content ?? msg.message ?? '';
        const isDuplicate = this.messages.some(
          m =>
            m.content === msgContent && m.senderType === (msg.senderType as any) && m.isFromAdmin === msg.isFromAdmin && !(m as any)._temp,
        );

        if (!isDuplicate) {
          console.warn('[CustomerSupport] ✅ Adding message to current conversation');
          // Add message to current chat
          this.messages.push(this.mapChatMessageToDisplayMessage(msg));
          this.scheduleScrollToBottom();
        } else {
          console.warn('[CustomerSupport] ⚠️ Duplicate message detected, skipping');
        }

        // Mark as read immediately since we're viewing it
        // Also reset unread count to 0 for currently viewed conversation
        conversation.unreadCount = 0;
        this.http.put(`/api/support/tickets/${msg.conversationId}/messages/mark-as-read`, {}).subscribe();
      } else {
        // For conversations not being viewed, unreadCount is already updated from WebSocket payload
        // No need to increment manually anymore
        console.warn(
          `[CustomerSupport] 💬 Message for conversation #${msg.conversationId ?? 'unknown'} (unreadCount: ${conversation.unreadCount})`,
        );
      }
      this.sortConversations();
    } else {
      // New conversation - reload the list
      console.warn('[CustomerSupport] 🆕 New conversation detected, reloading...');
      this.loadConversations();
    }
    this.cdr.detectChanges();
  }

  private sortConversations(): void {
    this.conversations.sort((a, b) => {
      // 1. Priority: Status (OPEN/IN_PROGRESS > CLOSED)
      const aClosed = a.status === 'CLOSED';
      const bClosed = b.status === 'CLOSED';

      if (aClosed && !bClosed) return 1; // a is closed, put it after b
      if (!aClosed && bClosed) return -1; // b is closed, put a before b

      // 2. Priority: Last Message Time (Newest first)
      return new Date(b.lastMessageAt).getTime() - new Date(a.lastMessageAt).getTime();
    });
  }

  private scheduleScrollToBottom(force = false): void {
    if (this.scrollPending) return;

    this.scrollPending = true;

    // Run outside Angular zone for better performance
    this.ngZone.runOutsideAngular(() => {
      requestAnimationFrame(() => {
        this.ngZone.run(() => {
          this.performScroll(force);
          this.scrollPending = false;
        });
      });
    });
  }

  private performScroll(force = false): void {
    try {
      if (!this.chatMessagesContainer) return;

      const container = this.chatMessagesContainer.nativeElement;
      const scrollHeight = container.scrollHeight;
      const clientHeight = container.clientHeight;
      const scrollTop = container.scrollTop;

      // Calculate if user is near bottom (within 100px)
      const isNearBottom = scrollHeight - scrollTop - clientHeight < 100;

      // Scroll conditions:
      // 1. Force scroll (e.g., new message, loading history, or session ended)
      // 2. User is near bottom and hasn't manually scrolled up
      if (force || (isNearBottom && !this.userScrolled)) {
        container.scrollTo({
          top: scrollHeight,
          behavior: force ? 'auto' : 'smooth',
        });
        this.lastScrollHeight = scrollHeight;

        // Reset userScrolled after forced scroll
        if (force) {
          this.userScrolled = false;
        }
      }
    } catch (err) {
      // Ignore scroll errors
    }
  }

  private mapChatMessageToDisplayMessage(msg: ChatMessage): Message {
    const ts = msg.timestamp ? (typeof msg.timestamp === 'string' ? new Date(msg.timestamp) : msg.timestamp) : new Date();
    return {
      conversationId: msg.conversationId!,
      senderIdentifier: msg.senderIdentifier ?? '',
      senderType: (msg.senderType as any) ?? 'USER',
      content: msg.content ?? msg.message ?? '',
      createdAt: ts.toISOString(),
      isFromAdmin: msg.isFromAdmin,
    };
  }
}
