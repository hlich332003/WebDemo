import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { WebSocketService, ChatMessage } from 'app/shared/services/websocket.service';
import { AccountService } from 'app/core/auth/account.service';
import { HttpClient } from '@angular/common/http';
import { SupportMessageDTO, SupportTicketDTO } from 'app/admin/customer-support/support.model';

type ViewMode = 'MENU' | 'CHAT' | 'CREATE_TICKET';

@Component({
  selector: 'jhi-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.scss'],
})
export class ChatWidgetComponent implements OnInit, OnDestroy, AfterViewChecked {
  // Public fields first (ESLint member-ordering)
  // public injected services (also public fields)
  public webSocketService = inject(WebSocketService);

  public isOpen = false;
  public viewMode: ViewMode = 'MENU';
  public isConnected = false; // Track WebSocket connection state

  public messages: ChatMessage[] = [];
  public newMessage = '';
  public currentTicket: SupportTicketDTO | null = null;
  public hasUnreadMessages = false;
  public isLoading = false;
  public sessionEnded = false;

  public ticketTitle = '';
  public ticketDescription = '';
  public isCreatingTicket = false;

  public hasMoreMessages = false; // for infinite scroll

  // Private fields after public
  @ViewChild('chatMessagesContainer')
  private chatMessagesContainer!: ElementRef;

  private notificationAudio = new Audio('content/sounds/notification.mp3');
  private accountService = inject(AccountService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();

  private pageSize = 20;

  ngOnInit(): void {
    // 0. Subscribe to WebSocket connection state
    this.webSocketService.connectionState$.pipe(takeUntil(this.destroy$)).subscribe((connected: boolean) => {
      this.isConnected = connected;
      console.warn('[ChatWidget] WebSocket connected:', connected);
      this.cdr.detectChanges();
    });

    // 1. Subscribe nhận tin nhắn
    this.webSocketService.chatMessage$.pipe(takeUntil(this.destroy$)).subscribe((msg: ChatMessage) => {
      // msg is always defined by the stream
      if (msg.type === 'SESSION_ENDED') {
        this.sessionEnded = true;
        const systemMsg: ChatMessage = {
          ...msg,
          isFromAdmin: true,
          type: 'system',
          message: msg.message ?? 'Phiên hỗ trợ đã kết thúc',
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
          // connect with token for logged in user
          const token = localStorage.getItem('jhi-authenticationToken') ?? sessionStorage.getItem('jhi-authenticationToken');
          if (token) this.webSocketService.connect(token);
        } else {
          // guest flow: ensure guest id exists and connect using endpoint param if backend supports
          let guestId = localStorage.getItem('chat_uid');
          if (!guestId) {
            guestId = crypto.randomUUID();
            localStorage.setItem('chat_uid', guestId);
          }
          // connect as guest by appending query param 'guestId' (backend must accept it)
          this.webSocketService.connect(`/websocket?guest=${guestId}`);
        }
      });

    // Do NOT disconnect in ngOnDestroy; MainComponent manages socket lifecycle
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    // Keep connection (MainComponent owns it)
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
    this.http.get<SupportTicketDTO | null>('/api/support/tickets/current').subscribe({
      next: ticket => {
        if (ticket) {
          this.currentTicket = ticket;
          this.viewMode = 'CHAT';
          this.loadChatHistory(ticket.id);
          // ensure we open & subscribe to the conversation topic
          this.webSocketService.openChat(ticket.id);
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
      description: 'Chat trực tiếp',
    };

    this.http.post<SupportTicketDTO>('/api/support/tickets', defaultTicket).subscribe({
      next: ticket => {
        this.currentTicket = ticket;
        this.viewMode = 'CHAT';
        // open websocket subscription for the new ticket
        this.webSocketService.openChat(ticket.id);
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
            isFromAdmin: false,
            senderType: 'USER',
            timestamp: new Date(),
          },
        ];
        // open socket for this newly created conversation
        this.webSocketService.openChat(ticket.id);
        this.ticketTitle = '';
        this.ticketDescription = '';
        this.isCreatingTicket = false;
        this.viewMode = 'CHAT';
      },
      error: () => (this.isCreatingTicket = false),
    });
  }

  // Update template to call this on scroll (we will modify HTML separately)
  onScroll(): void {
    try {
      const el = this.chatMessagesContainer?.nativeElement;
      if (!el || this.isLoading || !this.currentTicket) return;
      const scrollTop = el.scrollTop;
      if (scrollTop <= 10 && this.hasMoreMessages) {
        this.loadMoreMessages(this.currentTicket.id);
      }
    } catch (e) {
      // noop
    }
  }

  // Load initial history (most recent page)
  loadChatHistory(ticketId: number): void {
    this.isLoading = true;
    // initial load - get the latest 'pageSize' messages
    this.http.get<SupportMessageDTO[]>(`/api/support/tickets/${ticketId}/messages?limit=${this.pageSize}`).subscribe(
      messages => {
        // Server should return newest messages; we render in ascending order
        const mapped: ChatMessage[] = messages.map(m => ({
          content: m.message,
          message: m.message,
          isFromAdmin: m.isFromAdmin,
          timestamp: new Date(m.createdAt),
          senderType: (m.isFromAdmin ? 'ADMIN' : 'USER') as 'ADMIN' | 'USER',
        }));

        // If server returns newest-first, reverse to show oldest at top
        // Try to detect ordering: if first timestamp > last timestamp => reverse
        if (mapped.length > 1 && mapped[0].timestamp! > mapped[mapped.length - 1].timestamp!) {
          mapped.reverse();
        }

        this.messages = mapped;

        // Determine if there are more messages before (server should indicate; fallback based on length)
        this.hasMoreMessages = messages.length === this.pageSize;

        this.isLoading = false;
        this.scrollToBottom();
        this.cdr.detectChanges();
      },
      () => {
        this.isLoading = false;
      },
    );
  }

  // Load older messages when user scrolls up
  loadMoreMessages(ticketId: number): void {
    if (!this.messages.length) return;
    const oldest = this.messages[0];
    // normalize timestamp to ISO string safely
    const oldestDate = oldest.timestamp instanceof Date ? oldest.timestamp : new Date(oldest.timestamp as any);
    const beforeTime = oldestDate.toISOString();
    this.isLoading = true;

    const el = this.chatMessagesContainer?.nativeElement;
    const prevScrollHeight = el ? el.scrollHeight : 0;

    this.http
      .get<SupportMessageDTO[]>(`/api/support/tickets/${ticketId}/messages?before=${encodeURIComponent(beforeTime)}&limit=${this.pageSize}`)
      .subscribe(
        more => {
          const mapped: ChatMessage[] = more.map(m => ({
            content: m.message,
            message: m.message,
            isFromAdmin: m.isFromAdmin,
            timestamp: new Date(m.createdAt),
            senderType: (m.isFromAdmin ? 'ADMIN' : 'USER') as 'ADMIN' | 'USER',
          }));

          // If server returned newest-first, reverse
          if (mapped.length > 1 && mapped[0].timestamp! > mapped[mapped.length - 1].timestamp!) {
            mapped.reverse();
          }

          // prepend
          this.messages = [...mapped, ...this.messages];

          // update flag
          this.hasMoreMessages = more.length === this.pageSize;
          this.isLoading = false;

          // restore scroll position
          setTimeout(() => {
            try {
              if (el) {
                el.scrollTop = el.scrollHeight - prevScrollHeight;
              }
            } catch (_e) {
              // noop
            }
          }, 50);

          this.cdr.detectChanges();
        },
        () => {
          this.isLoading = false;
        },
      );
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || this.sessionEnded) return;

    const content = this.newMessage.trim();

    const message: ChatMessage = {
      content,
      message: content,
      timestamp: new Date(),
      isFromAdmin: false,
      senderType: 'USER',
    };
    // Do NOT optimistic push here. The UI should update from the WebSocket stream only.
    // Send payload to backend; backend will broadcast the saved message back to all subscribers.
    this.webSocketService.sendMessage({
      conversationId: this.currentTicket?.id,
      content,
    });

    // Clear input immediately for better UX; message will appear when server broadcasts it.
    this.newMessage = '';
    // keep scroll handling to let incoming message scroll when received
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    try {
      setTimeout(() => {
        try {
          this.chatMessagesContainer.nativeElement.scrollTop = this.chatMessagesContainer.nativeElement.scrollHeight;
        } catch (_err: unknown) {
          // ignore if element not ready
        }
      }, 50);
    } catch (_err: unknown) {
      /* ignore */
    }
  }

  private playNotificationSound(): void {
    this.notificationAudio.currentTime = 0;
    this.notificationAudio.play().catch((err: unknown) => console.warn('[ChatWidget] play sound failed', err));
  }
}
