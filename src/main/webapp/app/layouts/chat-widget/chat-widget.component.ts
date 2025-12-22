import {
  Component,
  inject,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  ViewChild,
  ElementRef,
  AfterViewChecked,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  WebSocketService,
  ChatMessage,
} from 'app/shared/services/websocket.service';
import { AccountService } from 'app/core/auth/account.service';
import { HttpClient } from '@angular/common/http';
import {
  SupportTicketDTO,
  SupportMessageDTO,
} from 'app/admin/customer-support/support.model';

type ViewMode = 'MENU' | 'CHAT' | 'CREATE_TICKET';

@Component({
  selector: 'jhi-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.scss'],
})
export class ChatWidgetComponent
  implements OnInit, OnDestroy, AfterViewChecked
{
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

  public webSocketService = inject(WebSocketService);
  private accountService = inject(AccountService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.webSocketService.chatMessage$
      .pipe(takeUntil(this.destroy$))
      .subscribe((msg) => {
        if (msg.type === 'SESSION_ENDED') {
          this.sessionEnded = true;
          this.messages.push({ ...msg, isFromAdmin: true, type: 'system' });
        } else {
          this.messages.push(msg);
          if (!this.isOpen) {
            this.hasUnreadMessages = true;
          }
          this.playNotificationSound();
        }
        this.cdr.detectChanges();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.hasUnreadMessages = false;
      if (!this.currentTicket && !this.sessionEnded) {
        this.checkActiveTicket();
      }
    }
  }

  checkActiveTicket(): void {
    this.isLoading = true;
    this.http.get<SupportTicketDTO>('/api/support/tickets/current').subscribe({
      next: (ticket) => {
        this.currentTicket = ticket;
        this.loadChatHistory(ticket.id);
        this.viewMode = 'CHAT';
        this.isLoading = false;
      },
      error: () => {
        this.currentTicket = null;
        this.viewMode = 'MENU';
        this.isLoading = false;
      },
    });
  }

  startDirectChat(): void {
    this.viewMode = 'CHAT';
    this.messages = [];
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

    this.http
      .post<SupportTicketDTO>('/api/support/tickets', ticketData)
      .subscribe({
        next: (ticket) => {
          this.currentTicket = ticket;
          this.messages = [
            {
              message: this.ticketDescription,
              timestamp: new Date(),
              isFromAdmin: false,
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
    this.http
      .get<SupportMessageDTO[]>(`/api/support/tickets/${ticketId}/messages`)
      .subscribe((messages) => {
        this.messages = messages.map((m) => ({
          message: m.message,
          isFromAdmin: m.isFromAdmin,
          timestamp: new Date(m.createdAt),
        }));
      });
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || this.sessionEnded) return;
    const message: ChatMessage = {
      message: this.newMessage.trim(),
      timestamp: new Date(),
      isFromAdmin: false,
    };
    this.messages.push(message);
    this.webSocketService.sendMessageToAdmin(message.message);
    this.newMessage = '';
  }

  private scrollToBottom(): void {
    try {
      if (this.chatMessagesContainer) {
        this.chatMessagesContainer.nativeElement.scrollTop =
          this.chatMessagesContainer.nativeElement.scrollHeight;
      }
    } catch (err) {
      /* ignore */
    }
  }

  private playNotificationSound(): void {
    const audio = new Audio('content/sounds/notification.mp3');
    audio.play().catch(() => {
      /* ignore */
    });
  }
}
