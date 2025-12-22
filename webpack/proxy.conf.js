function setupProxy({ tls }) {
  const onProxyError = (err, req, res) => {
    const errorCode = err.code || 'UNKNOWN';
    if (errorCode === 'ECONNRESET' || errorCode === 'ECONNREFUSED' || errorCode === 'ETIMEDOUT') {
      console.error(`[PROXY] Connection to backend at http${tls ? 's' : ''}://localhost:8080 refused.`);
      return;
    }
    console.error('[PROXY] Error:', err);
    if (res && !res.headersSent) {
      res.writeHead(502, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Proxy Error', code: errorCode, message: err.message }));
    }
  };

  const proxy = [
    {
      context: ['/api', '/services', '/management', '/v3/api-docs', '/h2-console', '/health', '/ws'],
      target: `http${tls ? 's' : ''}://localhost:8080`,
      secure: false,
      changeOrigin: tls,
      ws: true,
      logLevel: 'debug',
      onError: onProxyError,
    },
  ];

  return proxy;
}

module.exports = setupProxy;
