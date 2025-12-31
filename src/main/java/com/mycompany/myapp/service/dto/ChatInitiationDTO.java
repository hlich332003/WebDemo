package com.mycompany.myapp.service.dto;

import java.util.List;

public class ChatInitiationDTO {

    private SupportTicketDTO ticket;
    private List<SupportMessageDTO> messages;

    public ChatInitiationDTO(SupportTicketDTO ticket, List<SupportMessageDTO> messages) {
        this.ticket = ticket;
        this.messages = messages;
    }

    public SupportTicketDTO getTicket() {
        return ticket;
    }

    public void setTicket(SupportTicketDTO ticket) {
        this.ticket = ticket;
    }

    public List<SupportMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<SupportMessageDTO> messages) {
        this.messages = messages;
    }
}
