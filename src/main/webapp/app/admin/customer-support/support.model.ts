import { TicketStatus } from '../../entities/enumerations/ticket-status.model';

export interface SupportTicketDTO {
  id: number;
  userEmail: string;
  title: string;
  status: TicketStatus;
  assignedTo?: string;
  createdDate: string;
  lastModifiedDate: string;
  closedAt?: string;
  unreadCount?: number;
}

export interface SupportMessageDTO {
  id?: number;
  ticketId?: number;
  senderEmail?: string;
  message: string;
  isFromAdmin: boolean;
  createdAt: string;
  isRead?: boolean;
}
