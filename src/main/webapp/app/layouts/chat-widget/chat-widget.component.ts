import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { WebSocketService, ChatMessage } from 'app/shared/services/websocket.service';
import { AccountService } from 'app/core/auth/account.service';
import { HttpClient } from '@angular/common/http';
import { SupportTicketDTO, SupportMessageDTO } from 'app/admin/customer-support/support.model';

type ViewMode = 'MENU' | 'CHAT' | 'CREATE_TICKET';

@Component({
  selector: 'jhi-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.scss'],
})
export class ChatWidgetComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('chatMessagesContainer')
  private chatMessagesContainer!: ElementRef;

  isOpen = false;
  viewMode: ViewMode = 'MENU';

  messages: ChatMessage[] = [];
  newMessage = '';
  currentTicket: SupportTicketDTO | null = null;
  hasUnreadMessages = false;
  isLoading = false;
  sessionEnded = false;

  ticketTitle = '';
  ticketDescription = '';
  isCreatingTicket = false;

  private notificationAudio = new Audio('content/sounds/notification.mp3');

  public webSocketService = inject(WebSocketService);
  private accountService = inject(AccountService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // 1. Subscribe nhận tin nhắn
    this.webSocketService.chatMessage$.pipe(takeUntil(this.destroy$)).subscribe(msg => {
      if (!msg) return;

      if (msg.type === 'SESSION_ENDED') {
        this.sessionEnded = true;
        const systemMsg: ChatMessage = {
          ...msg,
          isFromAdmin: true,
          type: 'system',
          message: msg.message || 'Phiên hỗ trợ đã kết thúc',
        };
        this.messages.push(systemMsg);
      } else {
        this.messages.push(msg);
        if (!this.isOpen) {
          this.hasUnreadMessages = true;
        }
        this.playNotificationSound();
      }
      this.cdr.detectChanges();
    });

    // 2. Kiểm tra ticket cũ
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        if (account) {
          this.checkActiveTicket();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    // KHÔNG disconnect ở đây nữa, vì MainComponent quản lý kết nối
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;

    if (this.isOpen) {
      this.hasUnreadMessages = false;

      if (this.viewMode === 'CHAT' || this.currentTicket) {
        // Nếu có ticket mà chưa load tin nhắn, load lại
        if (this.currentTicket && this.messages.length === 0) {
          this.loadChatHistory(this.currentTicket.id);
        }
      }
    }
  }

  checkActiveTicket(): void {
    this.isLoading = true;
    this.http.get<SupportTicketDTO>('/api/support/tickets/current').subscribe({
      next: ticket => {
        if (ticket) {
          this.currentTicket = ticket;
          this.viewMode = 'CHAT';
          this.loadChatHistory(ticket.id);
        } else {
          this.currentTicket = null;
          if (this.viewMode === 'CHAT') {
            this.viewMode = 'MENU';
          }
        }
        this.isLoading = false;
      },
      error: () => {
        this.currentTicket = null;
        this.isLoading = false;
      },
    });
  }

  startDirectChat(): void {
    if (this.currentTicket) {
      this.viewMode = 'CHAT';
      return;
    }

    this.isLoading = true;
    const defaultTicket = {
      title: 'Hỗ trợ trực tuyến',
      description: 'Khách hàng yêu cầu chat trực tiếp',
    };

    this.http.post<SupportTicketDTO>('/api/support/tickets', defaultTicket).subscribe({
      next: ticket => {
        this.currentTicket = ticket;
        this.viewMode = 'CHAT';
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        alert('Không thể khởi tạo phiên chat. Vui lòng thử lại.');
      },
    });
  }

  showCreateTicketForm(): void {
    this.viewMode = 'CREATE_TICKET';
  }

  backToMenu(): void {
    this.viewMode = 'MENU';
  }

  createTicket(): void {
    if (!this.ticketTitle.trim() || !this.ticketDescription.trim()) return;
    this.isCreatingTicket = true;
    const ticketData = {
      title: this.ticketTitle.trim(),
      description: this.ticketDescription.trim(),
    };

    this.http.post<SupportTicketDTO>('/api/support/tickets', ticketData).subscribe({
      next: ticket => {
        this.currentTicket = ticket;
        this.messages = [
          {
            content: this.ticketDescription,
            message: this.ticketDescription,
            timestamp: new Date(),
            isFromAdmin: false,
            senderType: 'USER',
          },
        ];
        this.ticketTitle = '';
        this.ticketDescription = '';
        this.isCreatingTicket = false;
        this.viewMode = 'CHAT';
      },
      error: () => (this.isCreatingTicket = false),
    });
  }

  loadChatHistory(ticketId: number): void {
    this.http.get<SupportMessageDTO[]>(`/api/support/tickets/${ticketId}/messages`).subscribe(messages => {
      this.messages = messages.map(m => ({
        content: m.message,
        message: m.message,
        isFromAdmin: m.isFromAdmin,
        timestamp: new Date(m.createdAt),
        senderType: m.isFromAdmin ? 'ADMIN' : 'USER',
      }));
      this.scrollToBottom();
    });
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || this.sessionEnded) return;

    const content = this.newMessage.trim();

    const message: ChatMessage = {
      content: content,
      message: content,
      timestamp: new Date(),
      isFromAdmin: false,
      senderType: 'USER',
    };
    this.messages.push(message);

    this.webSocketService.sendMessage(content);

    this.newMessage = '';
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    try {
      setTimeout(() => {
        if (this.chatMessagesContainer) {
          this.chatMessagesContainer.nativeElement.scrollTop = this.chatMessagesContainer.nativeElement.scrollHeight;
        }
      }, 50);
    } catch (err) {
      /* ignore */
    }
  }

  private playNotificationSound(): void {
    this.notificationAudio.currentTime = 0;
    this.notificationAudio.play().catch(() => {});
  }
}
