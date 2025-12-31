// Global error handler for dev server to prevent crashes
process.on('uncaughtException', error => {
  // Only suppress connection-related errors
  if (
    error.code === 'ECONNRESET' ||
    error.code === 'ECONNREFUSED' ||
    error.code === 'ETIMEDOUT' ||
    error.code === 'EPIPE' ||
    error.code === 'ENOTFOUND' ||
    error.message?.includes('WebSocket') ||
    error.message?.includes('socket hang up') ||
    error.message?.includes('Connection closed')
  ) {
    // Silently ignore these errors - they're normal in development
    return;
  }
  // For other errors, log them but don't crash
  console.error('Uncaught Exception (handled):', error.message);
});

process.on('unhandledRejection', (reason, promise) => {
  // Silently ignore promise rejections related to connections
  if (
    reason?.code === 'ECONNRESET' ||
    reason?.code === 'ECONNREFUSED' ||
    reason?.code === 'ETIMEDOUT' ||
    reason?.code === 'EPIPE' ||
    reason?.message?.includes('WebSocket') ||
    reason?.message?.includes('socket hang up')
  ) {
    return;
  }
  console.error('Unhandled Rejection (handled):', reason);
});

module.exports = {};
