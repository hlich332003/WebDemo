import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ChatSocket } from 'app/core/websocket/chat.socket';
import { AccountService } from 'app/core/auth/account.service';
import { WebSocketService } from 'app/shared/services/websocket.service';
import { ChatService, ConversationResponse, ChatInitiationDTO } from 'app/shared/services/chat.service';
import { Account } from 'app/core/auth/account.model';
import { HttpClient } from '@angular/common/http';

type ViewMode = 'MENU' | 'CHAT' | 'CREATE_TICKET' | 'GUEST_INFO' | 'HISTORY';

interface MyTicket {
  id: number;
  title: string;
  status: string;
  lastModifiedDate: string;
}

@Component({
  selector: 'jhi-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.scss'],
})
export class ChatWidgetComponent implements OnInit, OnDestroy {
  // Services (private first)
  private accountService = inject(AccountService);
  private webSocketService = inject(WebSocketService);
  private chatService = inject(ChatService);
  private chatSocket = inject(ChatSocket);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private ngZone = inject(NgZone);
  private destroy$ = new Subject<never>();
  private notificationAudio = new Audio('content/sounds/notification.mp3');
  private scrollPending = false;
  private userScrolled = false;
  private lastScrollHeight = 0;

  @ViewChild('chatMessagesContainer')
  private chatMessagesContainer!: ElementRef;

  // State (public)
  public isOpen = false;
  public isMinimized = false;
  public viewMode: ViewMode = 'MENU';
  public isConnected = false;
  public isLoading = false;
  public sessionEnded = false;
  public hasUnreadMessages = false;

  // Models
  public messages: any[] = [];
  public newMessage = '';
  public currentConversation: ConversationResponse | null = null;
  public currentUser: Account | null = null;
  public myTickets: MyTicket[] = [];

  // Ticket Form
  public ticketTitle = '';
  public ticketDescription = '';
  public ticketContact = ''; // For guest email/phone
  public isCreatingTicket = false;

  // Guest Chat Info
  public guestChatContact = '';

  ngOnInit(): void {
    this.webSocketService.connectionState$.pipe(takeUntil(this.destroy$)).subscribe(connected => {
      this.isConnected = connected;
      if (connected && this.currentConversation) {
        this.chatSocket.openChat(this.currentConversation.id);
      }
      this.cdr.detectChanges();
    });

    this.chatSocket.messages$.pipe(takeUntil(this.destroy$)).subscribe(msg => {
      this.handleIncomingMessage(msg);
    });

    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        this.currentUser = account;
        if (!account) {
          this.ensureGuestSession();
        }
        // Try to restore previous conversation from session storage
        this.restoreConversation();
      });
  }

  ensureGuestSession(): void {
    let sessionId = localStorage.getItem('chat_session_id');
    if (!sessionId) {
      sessionId = crypto.randomUUID();
      localStorage.setItem('chat_session_id', sessionId);
    }
    localStorage.setItem('chat_guest_id', sessionId);
  }

  restoreConversation(): void {
    // For logged-in users, check with server for active conversation
    if (this.currentUser) {
      this.http.get<any>('/api/chat/conversations/my-active').subscribe({
        next: conversation => {
          if (conversation?.id) {
            console.warn('[ChatWidget] 🔄 Restored active conversation from server:', conversation.id);
            this.currentConversation = {
              id: conversation.id,
              status: conversation.status,
              userEmail: conversation.userEmail,
              title: 'Hỗ trợ trực tuyến',
              type: 'CHAT',
              assignedTo: null,
              createdDate: conversation.createdDate,
              lastModifiedDate: null,
              closedAt: null,
            };
            this.sessionEnded = conversation.status === 'CLOSED';

            // Load chat history - this will also set viewMode to CHAT
            this.loadChatHistory(conversation.id);

            // Subscribe to WebSocket if still OPEN
            if (conversation.status !== 'CLOSED' && this.isConnected) {
              this.chatSocket.openChat(conversation.id);
            }
          } else {
            console.warn('[ChatWidget] ℹ️ No active conversation found on server');
          }
        },
        error(err) {
          // 204 No Content or 401 Unauthorized - no active conversation
          if (err.status === 204 || err.status === 401) {
            console.warn('[ChatWidget] ℹ️ No active conversation (status:', err.status, ')');
          } else {
            console.error('[ChatWidget] ❌ Error checking active conversation:', err);
          }
        },
      });
    } else {
      // Guest users - check localStorage for session continuity
      const savedConvId = localStorage.getItem('chat_conversation_id');
      if (savedConvId) {
        const convId = parseInt(savedConvId, 10);
        if (!isNaN(convId)) {
          console.warn('[ChatWidget] 🔄 Guest: Restoring conversation from localStorage:', convId);
          this.chatService.getConversation(convId).subscribe({
            next: conversation => {
              console.warn('[ChatWidget] ✅ Guest conversation found:', {
                id: conversation.id,
                status: conversation.status,
              });
              this.currentConversation = conversation;
              this.sessionEnded = conversation.status === 'CLOSED';

              // Load chat history - this will also set viewMode to CHAT
              this.loadChatHistory(convId);

              if (conversation.status !== 'CLOSED' && this.isConnected) {
                this.chatSocket.openChat(convId);
              }
            },
            error(err) {
              console.warn('[ChatWidget] ⚠️ Guest conversation not found, clearing localStorage', err);
              localStorage.removeItem('chat_conversation_id');
            },
          });
        }
      }
    }
  }

  handleIncomingMessage(msg: any): void {
    console.warn('[ChatWidget] 📨 Incoming message:', {
      conversationId: msg.conversationId,
      type: msg.type,
      currentConversation: this.currentConversation?.id,
    });

    // Handle SESSION_ENDED first
    if (msg.type === 'SESSION_ENDED') {
      console.warn('[ChatWidget] 🔒 Session ended for conversation:', msg.conversationId);

      if (this.currentConversation?.id === msg.conversationId) {
        this.sessionEnded = true;

        // Update conversation status
        if (this.currentConversation) {
          this.currentConversation.status = 'CLOSED';
        }

        // Add system message to chat
        this.messages.push({
          conversationId: msg.conversationId,
          content: msg.message ?? 'Phiên hỗ trợ đã kết thúc.',
          senderType: 'SYSTEM',
          senderIdentifier: 'SYSTEM',
          isFromAdmin: true,
          createdAt: new Date().toISOString(),
        });

        // Reset scroll state to allow viewing history
        this.userScrolled = false;

        this.playNotificationSound();
        this.cdr.detectChanges();
        this.scheduleScrollToBottom(true);
      }
      return;
    }

    // Only process messages for current conversation
    if (this.currentConversation?.id !== msg.conversationId) {
      console.warn('[ChatWidget] ⚠️ Message for different conversation, ignoring');
      if (!this.isOpen) {
        this.hasUnreadMessages = true;
        this.playNotificationSound();
      }
      return;
    }

    // Remove temp message if exists
    const tempIndex = this.messages.findIndex(m => m._temp && m.content === (msg.content ?? msg.message));
    if (tempIndex >= 0) {
      console.warn('[ChatWidget] 🔄 Removing temp optimistic message');
      this.messages.splice(tempIndex, 1);
    }

    // Check for duplicates
    const msgContent = msg.content ?? msg.message ?? '';
    const msgExists = this.messages.some(
      m =>
        m.content === msgContent &&
        m.senderType === msg.senderType &&
        !m._temp &&
        Math.abs(new Date(m.createdAt).getTime() - new Date(msg.createdAt ?? new Date()).getTime()) < 2000,
    );

    if (!msgExists) {
      console.warn('[ChatWidget] ✅ Adding new message to chat');
      this.messages.push({
        ...msg,
        content: msgContent,
        createdAt: msg.createdAt ?? new Date().toISOString(),
      });

      // Play sound if message from admin and widget is closed
      if (msg.isFromAdmin && !this.isOpen) {
        this.playNotificationSound();
        this.hasUnreadMessages = true;
      }
    } else {
      console.warn('[ChatWidget] ⚠️ Duplicate message detected, skipping');
    }

    this.cdr.detectChanges();
    this.scheduleScrollToBottom();
  }

  ngOnDestroy(): void {
    this.destroy$.next(undefined as never);
    this.destroy$.complete();
  }

  toggleChat(): void {
    if (this.isMinimized) {
      // If minimized, restore it
      this.isMinimized = false;
      this.isOpen = true;
      this.scheduleScrollToBottom(true);
    } else if (this.isOpen) {
      // If open, minimize it instead of closing
      this.minimizeChat();
    } else {
      // If closed, open it
      this.isOpen = true;
      this.isMinimized = false;
      this.hasUnreadMessages = false;
      this.userScrolled = false;
      // Reset to menu on open, unless a conversation is active
      if (!this.currentConversation) {
        this.viewMode = 'MENU';
      } else {
        // Scroll to bottom when reopening chat
        this.scheduleScrollToBottom(true);
      }
    }
  }

  minimizeChat(): void {
    this.isMinimized = true;
    this.isOpen = false;
  }

  closeChat(): void {
    // Check if user is in an active chat session
    if (this.currentConversation && this.viewMode === 'CHAT' && !this.sessionEnded) {
      // Show confirmation dialog
      const confirmed = confirm(
        'Bạn đang trong phiên chat trực tiếp. Đóng cửa sổ sẽ kết thúc phiên chat.\n\nBạn có chắc chắn muốn đóng không?',
      );

      if (!confirmed) {
        return; // User cancelled
      }

      // User confirmed - end the session
      this.endChatSession();
    }

    // Close the chat widget completely
    this.isOpen = false;
    this.isMinimized = false;
  }

  endChatSession(): void {
    if (!this.currentConversation) return;

    console.warn('[ChatWidget] 🔒 Ending chat session:', this.currentConversation.id);

    // Call API to close conversation
    this.chatService.closeConversation(this.currentConversation.id).subscribe({
      next: () => {
        console.warn('[ChatWidget] ✅ Chat session ended successfully');
        this.sessionEnded = true;
        if (this.currentConversation) {
          this.currentConversation.status = 'CLOSED';
        }
        // Clear localStorage
        localStorage.removeItem('chat_conversation_id');
      },
      error: err => {
        console.error('[ChatWidget] ❌ Error ending chat session:', err);
      },
    });
  }

  onUserScroll(): void {
    if (!this.chatMessagesContainer) return;

    const container = this.chatMessagesContainer.nativeElement;
    const isAtBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 50;

    // User has scrolled up
    this.userScrolled = !isAtBottom;
  }

  initiateChat(): void {
    // If already have an active (not closed) conversation, just return to it
    if (this.currentConversation && this.currentConversation.status !== 'CLOSED') {
      console.warn('[ChatWidget] 📝 Returning to existing active conversation:', this.currentConversation.id);
      this.viewMode = 'CHAT';
      this.sessionEnded = false;
      this.userScrolled = false;
      this.scheduleScrollToBottom(true);
      return;
    }

    // For logged-in users, check server first
    if (this.currentUser) {
      console.warn('[ChatWidget] 🔍 Checking server for active conversation...');
      this.http.get<any>('/api/chat/conversations/my-active').subscribe({
        next: conversation => {
          if (conversation?.id && conversation.status === 'OPEN') {
            console.warn('[ChatWidget] ♻️ Found OPEN conversation on server:', conversation.id);
            this.currentConversation = {
              id: conversation.id,
              status: conversation.status,
              userEmail: conversation.userEmail,
              title: 'Hỗ trợ trực tuyến',
              type: 'CHAT',
              assignedTo: null,
              createdDate: conversation.createdDate,
              lastModifiedDate: null,
              closedAt: null,
            };
            this.sessionEnded = false;
            this.viewMode = 'CHAT';
            this.userScrolled = false;
            this.loadChatHistory(conversation.id);
            if (this.isConnected) {
              this.chatSocket.openChat(conversation.id);
            }
          } else {
            console.warn('[ChatWidget] 🆕 No active conversation on server, creating new one');
            this.startDirectChat();
          }
        },
        error: err => {
          if (err.status === 204) {
            console.warn('[ChatWidget] 🆕 No active conversation (204), creating new one');
            this.startDirectChat();
          } else {
            console.error('[ChatWidget] ❌ Error checking server:', err);
            alert('Không thể kết nối. Vui lòng thử lại.');
          }
        },
      });
      return;
    }

    // For guest users, check localStorage
    const savedConvId = localStorage.getItem('chat_conversation_id');
    if (savedConvId) {
      const convId = parseInt(savedConvId, 10);
      if (!isNaN(convId)) {
        console.warn('[ChatWidget] 🔍 Guest: Found saved conversation:', convId);
        this.chatService.getConversation(convId).subscribe({
          next: conversation => {
            if (conversation.status !== 'CLOSED') {
              console.warn('[ChatWidget] ♻️ Guest: Reusing OPEN conversation:', convId);
              this.currentConversation = conversation;
              this.sessionEnded = false;
              this.viewMode = 'CHAT';
              this.userScrolled = false;
              this.loadChatHistory(convId);
              if (this.isConnected) {
                this.chatSocket.openChat(convId);
              }
            } else {
              console.warn('[ChatWidget] ⚠️ Guest: Saved conversation is CLOSED, creating new');
              localStorage.removeItem('chat_conversation_id');
              this.viewMode = 'GUEST_INFO';
            }
          },
          error: () => {
            console.warn('[ChatWidget] ❌ Guest: Conversation not found, creating new');
            localStorage.removeItem('chat_conversation_id');
            this.viewMode = 'GUEST_INFO';
          },
        });
        return;
      }
    }

    // Guest without saved conversation
    console.warn('[ChatWidget] 🆕 Guest: No saved conversation, showing info form');
    this.viewMode = 'GUEST_INFO';
  }

  startDirectChat(): void {
    if (!this.currentUser && !this.guestChatContact.trim()) {
      return;
    }
    this.isLoading = true;
    this.sessionEnded = false; // Reset session ended flag
    this.userScrolled = false;
    const contact = this.currentUser ? undefined : this.guestChatContact.trim();
    this.chatService.createChatConversation(contact).subscribe({
      next: (initData: ChatInitiationDTO) => {
        this.currentConversation = initData.ticket;
        localStorage.setItem('chat_conversation_id', initData.ticket.id.toString());

        // Subscribe to WebSocket for real-time updates
        this.chatSocket.openChat(initData.ticket.id);

        // Set initial messages from the response (including welcome message)
        this.messages = initData.messages
          .map(m => ({
            ...m,
            isFromAdmin: m.isFromAdmin,
            content: m.message ?? m.content,
          }))
          .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

        this.viewMode = 'CHAT';
        this.isLoading = false;
        this.guestChatContact = ''; // Clear guest contact input
        this.scheduleScrollToBottom(true);
      },
      error: () => {
        this.isLoading = false;
        alert('Không thể tạo phiên chat. Vui lòng thử lại.');
      },
    });
  }

  showCreateTicketForm(): void {
    this.viewMode = 'CREATE_TICKET';
  }

  createTicket(): void {
    if (!this.ticketTitle.trim() || !this.ticketDescription.trim() || (!this.currentUser && !this.ticketContact.trim())) {
      return;
    }
    this.isCreatingTicket = true;
    const email = this.currentUser ? this.currentUser.email : this.ticketContact.trim();
    this.chatService.createSupportTicket(this.ticketTitle.trim(), this.ticketDescription.trim(), email).subscribe({
      next: ticket => {
        alert(`Ticket #${ticket.id} đã được tạo thành công!`);
        this.backToMenu();
      },
      error: () => {
        this.isCreatingTicket = false;
        alert('Tạo ticket thất bại. Vui lòng thử lại.');
      },
    });
  }

  showHistory(): void {
    this.isLoading = true;
    this.viewMode = 'HISTORY';
    this.http.get<MyTicket[]>('/api/support/tickets/my').subscribe({
      next: data => {
        this.myTickets = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  selectTicketFromHistory(ticket: { id: number }): void {
    this.isLoading = true;
    this.userScrolled = false;
    this.chatService.getConversation(ticket.id).subscribe({
      next: conversation => {
        this.currentConversation = conversation;
        this.sessionEnded = conversation.status === 'CLOSED';
        this.chatSocket.openChat(conversation.id);
        this.loadChatHistory(conversation.id);
        this.viewMode = 'CHAT';
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.backToMenu();
      },
    });
  }

  backToMenu(): void {
    this.viewMode = 'MENU';
    // Don't clear currentConversation and messages - keep them for returning to chat
    this.isCreatingTicket = false;
    this.guestChatContact = '';
    this.ticketContact = '';
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.currentConversation || this.sessionEnded) {
      console.warn('[ChatWidget] Cannot send message:', {
        hasMessage: !!this.newMessage.trim(),
        hasConversation: !!this.currentConversation,
        sessionEnded: this.sessionEnded,
      });
      return;
    }

    const content = this.newMessage.trim();
    const conversationId = this.currentConversation.id;

    if (!this.isConnected) {
      console.warn('[ChatWidget] WebSocket not connected, cannot send message');
      return;
    }

    console.warn('[ChatWidget] 📤 Sending message:', {
      conversationId,
      contentLength: content.length,
      isConnected: this.isConnected,
    });

    // Add message to UI immediately for better UX (optimistic update)
    const tempMsg = {
      conversationId,
      content,
      senderType: this.currentUser ? 'USER' : 'GUEST',
      senderIdentifier: this.currentUser?.email ?? 'Guest',
      isFromAdmin: false,
      createdAt: new Date().toISOString(),
      _temp: true, // Mark as temporary
    };
    this.messages.push(tempMsg);
    this.userScrolled = false; // Reset user scroll when sending
    this.scheduleScrollToBottom(true);

    this.chatSocket.sendMessage({
      conversationId,
      content,
    });

    this.newMessage = '';
  }

  loadChatHistory(conversationId: number): void {
    console.warn(`[ChatWidget] 📖 Loading chat history for conversation #${conversationId}`);
    this.isLoading = true;
    this.userScrolled = false;
    this.chatService.getMessages(conversationId).subscribe({
      next: messages => {
        console.warn(`[ChatWidget] ✅ Loaded ${messages.length} messages from DB for conversation #${conversationId}`);

        // Clear existing messages to avoid duplicates
        this.messages = [];

        // Map messages and sort by creation time
        this.messages = messages
          .map(m => ({
            ...m,
            isFromAdmin: m.isFromAdmin,
            content: m.message ?? m.content, // Ensure we have content field
            createdAt: m.createdAt,
            conversationId,
          }))
          .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

        console.warn(`[ChatWidget] 📊 Total messages in UI: ${this.messages.length}`);

        // Set view mode to CHAT to show the conversation
        this.viewMode = 'CHAT';
        this.isLoading = false;

        // Force change detection and scroll
        this.cdr.detectChanges();
        this.scheduleScrollToBottom(true);
      },
      error: err => {
        console.error('[ChatWidget] ❌ Failed to load chat history:', err);
        this.isLoading = false;
      },
    });
  }

  closeConversation(): void {
    if (this.currentConversation && confirm('Bạn có chắc muốn kết thúc phiên chat này không?')) {
      this.chatService.closeConversation(this.currentConversation.id).subscribe({
        next: () => {
          this.sessionEnded = true;
        },
        error() {
          alert('Không thể đóng phiên chat. Vui lòng thử lại.');
        },
      });
    }
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
      // 1. Force scroll (e.g., new message or session ended)
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

  private playNotificationSound(): void {
    this.notificationAudio.currentTime = 0;
    this.notificationAudio.play().catch(() => {
      // Ignore audio play errors (e.g., user interaction required)
    });
  }
}
