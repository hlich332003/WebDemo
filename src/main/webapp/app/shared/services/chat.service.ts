import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { AccountService } from 'app/core/auth/account.service';

// This DTO is now returned when initiating a chat
export interface ChatInitiationDTO {
  ticket: ConversationResponse;
  messages: any[]; // Assuming messages are of 'any' type for now
}

export interface ConversationResponse {
  id: number;
  userEmail: string;
  status: string;
  title?: string;
  type?: string;
  assignedTo?: string | null;
  createdDate?: string;
  lastModifiedDate?: string | null;
  closedAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);
  private applicationConfigService = inject(ApplicationConfigService);
  private accountService = inject(AccountService);

  private resourceUrl = this.applicationConfigService.getEndpointFor('api');

  /**
   * Creates a new CHAT conversation.
   * Handles both Guest and authenticated User.
   * @param contact Optional contact info (email/phone) for guests
   */
  createChatConversation(contact?: string): Observable<ChatInitiationDTO> {
    const payload: any = {};

    let sessionId = localStorage.getItem('chat_session_id');
    if (!sessionId) {
      sessionId = crypto.randomUUID();
      localStorage.setItem('chat_session_id', sessionId);
    }
    payload.sessionId = sessionId;

    if (contact) {
      payload.contact = contact;
    }

    return this.http.post<ChatInitiationDTO>(`${this.resourceUrl}/chat/conversations`, payload);
  }

  /**
   * Creates a new SUPPORT TICKET.
   */
  createSupportTicket(title: string, description: string, email?: string): Observable<ConversationResponse> {
    const payload: any = {
      title,
      description,
      email,
    };
    return this.http.post<ConversationResponse>(`${this.resourceUrl}/support/tickets`, payload);
  }

  /**
   * Retrieves an existing conversation (ticket) by ID.
   */
  getConversation(id: number): Observable<ConversationResponse> {
    return this.http.get<ConversationResponse>(`${this.resourceUrl}/support/tickets/${id}`);
  }

  /**
   * Loads message history.
   */
  getMessages(conversationId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.resourceUrl}/support/tickets/${conversationId}/messages`);
  }

  /**
   * Closes a conversation/ticket.
   */
  closeConversation(conversationId: number): Observable<void> {
    return this.http.post<void>(`${this.resourceUrl}/support/tickets/${conversationId}/close`, {});
  }
}
