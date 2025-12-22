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
import { CommonModule, SlicePipe } from '@angular/common';
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
import { SupportTicketDTO, SupportMessageDTO } from './support.model';
import { Account } from 'app/core/auth/account.model';

@Component({
  selector: 'jhi-customer-support',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule, SlicePipe],
  templateUrl: './customer-support.component.html',
  styleUrls: ['./customer-support.component.scss'],
})
export class CustomerSupportComponent
  implements OnInit, OnDestroy, AfterViewChecked
{
  @ViewChild('chatMessagesContainer')
  private chatMessagesContainer!: ElementRef;

  private webSocketService = inject(WebSocketService);
  private accountService = inject(AccountService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();

  isConnected = false;
  newMessage = '';
  tickets: SupportTicketDTO[] = [];
  selectedTicket: SupportTicketDTO | null = null;
  messages: SupportMessageDTO[] = [];
  isLoadingMessages = false;
  adminUser: Account | null = null;

  ngOnInit(): void {
    this.accountService.getAuthenticationState().subscribe((account) => {
      this.adminUser = account;
    });

    if (!this.accountService.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_CSKH'])) {
      return;
    }

    this.loadTickets();

    this.webSocketService.connectionState$
      .pipe(takeUntil(this.destroy$))
      .subscribe((isConnected) => {
        this.isConnected = isConnected;
      });

    this.webSocketService.chatMessage$
      .pipe(takeUntil(this.destroy$))
      .subscribe((msg) => {
        if (msg && !msg.isFromAdmin) {
          const ticket = this.tickets.find((t) => t.userEmail === msg.user);
          if (ticket) {
            if (this.selectedTicket?.id === ticket.id) {
              this.messages.push(this.mapChatMessageToDTO(msg));
              this.markMessagesAsRead(ticket.id);
              this.scrollToBottom();
            } else {
              ticket.unreadCount = (ticket.unreadCount ?? 0) + 1;
            }
          } else {
            this.loadTickets();
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

  loadTickets(): void {
    this.http.get<SupportTicketDTO[]>('/api/support/tickets').subscribe({
      next: (tickets) => {
        this.tickets = tickets.sort(
          (a, b) =>
            new Date(b.lastModifiedDate).getTime() -
            new Date(a.lastModifiedDate).getTime(),
        );
        if (this.selectedTicket) {
          this.selectedTicket =
            this.tickets.find((t) => t.id === this.selectedTicket!.id) ?? null;
        }
      },
    });
  }

  selectTicket(ticket: SupportTicketDTO): void {
    this.selectedTicket = ticket;
    this.messages = [];
    this.isLoadingMessages = true;
    this.http
      .get<SupportMessageDTO[]>(`/api/support/tickets/${ticket.id}/messages`)
      .subscribe({
        next: (messages) => {
          this.messages = messages;
          this.markMessagesAsRead(ticket.id);
          this.isLoadingMessages = false;
          this.scrollToBottom();
        },
        error: () => (this.isLoadingMessages = false),
      });
  }

  sendMessage(): void {
    if (
      this.newMessage.trim() === '' ||
      !this.isConnected ||
      !this.selectedTicket
    ) {
      return;
    }

    const messagePayload = { message: this.newMessage };
    this.webSocketService.sendMessageToUser(
      this.selectedTicket.userEmail,
      JSON.stringify(messagePayload),
    );

    const tempMessage: SupportMessageDTO = {
      ticketId: this.selectedTicket.id,
      senderEmail: this.adminUser?.email,
      message: this.newMessage,
      isFromAdmin: true,
      createdAt: new Date().toISOString(),
      isRead: true,
    };
    this.messages.push(tempMessage);
    this.newMessage = '';
    this.scrollToBottom();
  }

  closeTicket(ticketId: number): void {
    if (confirm('Bạn có chắc muốn đóng phiếu hỗ trợ này không?')) {
      this.http.post(`/api/support/tickets/${ticketId}/close`, {}).subscribe({
        next: () => {
          this.loadTickets();
          if (this.selectedTicket?.id === ticketId) {
            this.selectedTicket = null;
          }
        },
      });
    }
  }

  private markMessagesAsRead(ticketId: number): void {
    this.http
      .put(`/api/support/tickets/${ticketId}/messages/mark-as-read`, {})
      .subscribe({
        next: () => {
          const ticket = this.tickets.find((t) => t.id === ticketId);
          if (ticket) {
            ticket.unreadCount = 0;
          }
        },
      });
  }

  private scrollToBottom(): void {
    this.cdr.detectChanges();
    setTimeout(() => {
      if (this.chatMessagesContainer) {
        this.chatMessagesContainer.nativeElement.scrollTop =
          this.chatMessagesContainer.nativeElement.scrollHeight;
      }
    }, 50);
  }

  private mapChatMessageToDTO(msg: ChatMessage): SupportMessageDTO {
    return {
      senderEmail: msg.user,
      message: msg.message,
      isFromAdmin: msg.isFromAdmin,
      createdAt: msg.timestamp.toISOString(),
      isRead: false,
    };
  }
}
