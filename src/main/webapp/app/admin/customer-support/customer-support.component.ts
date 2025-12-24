import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule, SlicePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { WebSocketService, ChatMessage } from 'app/shared/services/websocket.service';
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
  @ViewChild('chatMessagesContainer')
  private chatMessagesContainer!: ElementRef;

  private webSocketService = inject(WebSocketService);
  private accountService = inject(AccountService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();

  isConnected = false;
  adminUser: Account | null = null;
  conversations: Conversation[] = [];
  selectedConversation: Conversation | null = null;
  messages: Message[] = [];
  newMessage = '';
  isLoadingMessages = false;

  ngOnInit(): void {
    this.accountService.getAuthenticationState().subscribe(account => {
      this.adminUser = account;
    });

    if (!this.accountService.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_CSKH'])) {
      return;
    }

    this.loadConversations();

    this.webSocketService.connectionState$.pipe(takeUntil(this.destroy$)).subscribe(isConnected => {
      this.isConnected = isConnected;
    });

    this.webSocketService.cskhChatStream_.pipe(takeUntil(this.destroy$)).subscribe((msg: ChatMessage) => {
      if (msg && msg.senderType === 'USER') {
        const conversation = this.conversations.find(c => c.id === msg.conversationId);
        if (conversation) {
          conversation.lastMessageAt = msg.timestamp.toISOString();
          if (this.selectedConversation?.id === msg.conversationId) {
            this.messages.push(this.mapChatMessageToDisplayMessage(msg));
          } else {
            conversation.unreadCount = (conversation.unreadCount ?? 0) + 1;
          }
          this.sortConversations();
        } else {
          this.loadConversations();
        }
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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

    // Stub: Notify service we are opening this chat
    this.webSocketService.openChat(conversation.id);

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
    const content = this.newMessage;

    this.webSocketService.sendReplyAsCskh(conversationId, content);

    const tempMessage: Message = {
      conversationId: conversationId,
      senderIdentifier: this.adminUser?.email ?? 'CSKH',
      senderType: 'CSKH',
      content: content,
      createdAt: new Date().toISOString(),
    };
    this.messages.push(tempMessage);
    this.newMessage = '';
    this.scrollToBottom();
  }

  closeConversation(conversationId: number): void {
    if (confirm('Bạn có chắc muốn đóng cuộc trò chuyện này không?')) {
      // Stub: Notify service we are closing this chat
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

  private sortConversations(): void {
    this.conversations.sort((a, b) => new Date(b.lastMessageAt).getTime() - new Date(a.lastMessageAt).getTime());
  }

  private scrollToBottom(): void {
    this.cdr.detectChanges();
    setTimeout(() => {
      if (this.chatMessagesContainer) {
        this.chatMessagesContainer.nativeElement.scrollTop = this.chatMessagesContainer.nativeElement.scrollHeight;
      }
    }, 50);
  }

  private mapChatMessageToDisplayMessage(msg: ChatMessage): Message {
    return {
      conversationId: msg.conversationId!,
      senderIdentifier: msg.senderIdentifier!,
      senderType: msg.senderType,
      content: msg.content,
      createdAt: msg.timestamp.toISOString(),
    };
  }
}
