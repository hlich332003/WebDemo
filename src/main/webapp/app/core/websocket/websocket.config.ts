export const WS_CONFIG = {
  BASE_URL: '/websocket',
  RECONNECT_DELAY: 5000,
  HEARTBEAT_INCOMING: 25000,
  HEARTBEAT_OUTGOING: 25000,
  BROKER: {
    CHAT_SEND: '/app/chat.send',
    CHAT_REPLY: '/app/chat.reply',
    // dynamic conversation topic prefix: '/topic/chat/conversations/{conversationId}'
    CHAT_CONVERSATIONS_PREFIX: '/topic/chat/conversations',
    // keep legacy per-user queue name for backward compatibility
    USER_CHAT_QUEUE: '/user/queue/chat',
    NOTIFICATIONS_QUEUE: '/user/queue/notifications',
    ADMIN_NOTIFICATIONS: '/topic/admin/notifications',
  },
};
