export const WS_CONFIG = {
  BASE_URL: '/websocket',
  RECONNECT_DELAY: 5000,
  HEARTBEAT_INCOMING: 10000, // Match backend 10s heartbeat
  HEARTBEAT_OUTGOING: 10000, // Match backend 10s heartbeat
  BROKER: {
    CHAT_SEND: '/app/chat.send',
    CHAT_REPLY: '/app/chat.reply',
    // dynamic conversation topic prefix: '/topic/chat.{conversationId}'
    CHAT_CONVERSATIONS_PREFIX: '/topic/chat',
    // keep legacy per-user queue name for backward compatibility
    USER_CHAT_QUEUE: '/user/queue/chat',
    NOTIFICATIONS_QUEUE: '/user/queue/notifications',
    ADMIN_NOTIFICATIONS: '/topic/admin/notifications',
  },
};
