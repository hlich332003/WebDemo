function setupProxy({ tls }) {
  const serverResources = ['/api', '/services', '/management', '/v3/api-docs', '/h2-console', '/health'];
  const conf = [
    {
      context: serverResources,
      target: `http${tls ? 's' : ''}://localhost:8080`,
      secure: false,
      changeOrigin: tls,
    },
    {
      context: ['/ws'],
      target: `http${tls ? 's' : ''}://localhost:8080`,
      secure: false,
      ws: true,
    },
    {
      context: ['/websocket'],
      target: `http${tls ? 's' : ''}://localhost:8080`,
      secure: false,
      ws: true,
      changeOrigin: tls,
    },
  ];
  return conf;
}

module.exports = setupProxy;
