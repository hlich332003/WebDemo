import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule, SlicePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject, Subscription } from 'rxjs';
import { takeUntil, take } from 'rxjs/operators';
import { WebSocketService, ChatMessage } from 'app/shared/services/websocket.service';
import { WS_CONFIG } from 'app/core/websocket/websocket.config';
import { AccountService } from 'app/core/auth/account.service';
import { HttpClient } from '@angular/common/http';
import { Account } from 'app/core/auth/account.model';

// Simplified models for this component
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
  senderType: 'USER' | 'ADMIN' | 'SYSTEM' | 'CSKH';
  senderIdentifier: string;
  content: string;
  createdAt: string;
}

@Component({
  selector: 'jhi-customer-support',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule, SlicePipe],
  templateUrl: './customer-support.component.html',
  styleUrls: ['./customer-support.component.scss'],
})
export class CustomerSupportComponent implements OnInit, OnDestroy, AfterViewChecked {
  // Public state members first (ESLint member-ordering)
  isConnected = false;
  adminUser: Account | null = null;
  conversations: Conversation[] = [];
  selectedConversation: Conversation | null = null;
  messages: Message[] = [];
  newMessage = '';
  isLoadingMessages = false;

  // Private injected services after public members
  private webSocketService = inject(WebSocketService);
  private accountService = inject(AccountService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();

  // Private view child (after public fields)
  @ViewChild('chatMessagesContainer')
  private chatMessagesContainer!: ElementRef;

  private currentSub?: Subscription;

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
      // đảm bảo change detection cập nhật UI khi connection thay đổi
      try {
        this.cdr.detectChanges();
      } catch (e) {
        // ignore if change detection not available
      }
    });

    this.webSocketService.chatMessage$.pipe(takeUntil(this.destroy$)).subscribe((msg: ChatMessage) => {
      // msg is always defined by the stream
      if (msg.senderType === 'USER') {
        const conversation = this.conversations.find(c => c.id === msg.conversationId);
        if (conversation) {
          // normalize timestamp to string
          const ts = msg.timestamp ? (typeof msg.timestamp === 'string' ? new Date(msg.timestamp) : msg.timestamp) : new Date();
          conversation.lastMessageAt = ts instanceof Date ? ts.toISOString() : String(ts);
          if (this.selectedConversation?.id === msg.conversationId) {
            this.messages.push(this.mapChatMessageToDisplayMessage(msg));
          } else {
            // increment unread safely
            conversation.unreadCount = conversation.unreadCount ? conversation.unreadCount + 1 : 1;
          }
          this.sortConversations();
        } else {
          this.loadConversations();
        }
        this.cdr.detectChanges();
      }
    });

    // Connect WebSocket for ADMIN only after token available
    const token = localStorage.getItem('jhi-authenticationToken') ?? sessionStorage.getItem('jhi-authenticationToken');
    if (token) {
      this.webSocketService.connect(token);
    } else {
      // wait for authentication state and then connect once
      this.accountService
        .getAuthenticationState()
        .pipe(take(1))
        .subscribe(() => {
          const t = localStorage.getItem('jhi-authenticationToken') ?? sessionStorage.getItem('jhi-authenticationToken');
          if (t) {
            this.webSocketService.connect(t);
          }
        });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.currentSub?.unsubscribe();
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  loadConversations(): void {
    this.http.get<Conversation[]>('/api/chat/conversations').subscribe(convos => {
      this.conversations = convos;
      this.sortConversations();
    });
  }

  selectConversation(conversation: Conversation): void {
    this.selectedConversation = conversation;
    this.messages = [];
    this.isLoadingMessages = true;

    // Unsubscribe previous per-conversation subscription (if any)
    if (this.currentSub) {
      this.currentSub.unsubscribe();
      this.currentSub = undefined;
    }

    // Unsubscribe previous conversation topic if any and subscribe/open new at standardized topic
    // Let the global chatMessage$ handler (registered in ngOnInit) route incoming messages to conversations
    // Ensure the shared service subscribes to the canonical topic for this conversation
    this.webSocketService.openChat(conversation.id);
    // track unsubscribe for this conversation so we can unsubscribe when switching
    this.currentSub = new Subscription(() =>
      this.webSocketService.unsubscribe(`${WS_CONFIG.BROKER.CHAT_CONVERSATIONS_PREFIX}/${conversation.id}`),
    );

    this.http.get<Message[]>(`/api/chat/conversations/${conversation.id}/messages`).subscribe({
      next: messages => {
        this.messages = messages;
        if (conversation.unreadCount > 0) {
          conversation.unreadCount = 0;
        }
        this.isLoadingMessages = false;
        this.scrollToBottom();
      },
      error: () => (this.isLoadingMessages = false),
    });
  }

  sendMessage(): void {
    if (this.newMessage.trim() === '' || !this.selectedConversation) {
      return;
    }

    const conversationId = this.selectedConversation.id;
    const content = this.newMessage.trim();

    // send to backend; backend will broadcast saved message back to admin and user
    this.webSocketService.sendReplyAsCskh(conversationId, content);

    // Clear input; do NOT optimistic push to avoid duplicates
    this.newMessage = '';
    this.scrollToBottom();
  }

  closeConversation(conversationId: number): void {
    if (confirm('Bạn có chắc muốn đóng cuộc trò chuyện này không?')) {
      // Notify CS socket/service we are closing this chat (placeholder)
      this.webSocketService.closeChat(conversationId);

      this.http.post(`/api/chat/conversations/${conversationId}/close`, {}).subscribe({
        next: () => {
          this.loadConversations();
          if (this.selectedConversation?.id === conversationId) {
            this.selectedConversation = null;
          }
        },
      });
    }
  }

  // trackBy functions for ngFor (keep before private helpers)
  trackByConversationId(index: number, item: Conversation): number {
    return item.id;
  }

  trackByMessageId(index: number, item: Message): number | undefined {
    return item.id;
  }

  private sortConversations(): void {
    this.conversations.sort((a, b) => new Date(b.lastMessageAt).getTime() - new Date(a.lastMessageAt).getTime());
  }

  private scrollToBottom(): void {
    this.cdr.detectChanges();
    setTimeout(() => {
      try {
        // chatMessagesContainer is non-null after view init; access directly and catch runtime errors
        this.chatMessagesContainer.nativeElement.scrollTop = this.chatMessagesContainer.nativeElement.scrollHeight;
      } catch (e) {
        // ignore when element not ready
      }
    }, 50);
  }

  private mapChatMessageToDisplayMessage(msg: ChatMessage): Message {
    const ts = msg.timestamp ? (typeof msg.timestamp === 'string' ? new Date(msg.timestamp) : msg.timestamp) : new Date();
    return {
      conversationId: msg.conversationId!,
      senderIdentifier: msg.senderIdentifier ?? '',
      senderType: msg.senderType ?? 'USER',
      content: msg.content ?? msg.message ?? '',
      createdAt: ts instanceof Date ? ts.toISOString() : String(ts),
    };
  }
}
