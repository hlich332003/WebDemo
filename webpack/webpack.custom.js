const path = require('path');
const webpack = require('webpack');
const { merge } = require('webpack-merge');
const BrowserSyncPlugin = require('browser-sync-webpack-plugin');
const { BundleAnalyzerPlugin } = require('webpack-bundle-analyzer');
const WebpackNotifierPlugin = require('webpack-notifier');
const CopyWebpackPlugin = require('copy-webpack-plugin');

const environment = require('./environment');
const proxyConfig = require('./proxy.conf');

module.exports = async (config, options, targetOptions) => {
  // PLUGINS
  if (config.mode === 'development') {
    config.plugins.push(
      new WebpackNotifierPlugin({
        title: 'Web Demo',
        contentImage: path.join(__dirname, 'logo-jhipster.png'),
      }),
    );
  }

  // Cấu hình proxy cho webpack-dev-server
  const tls = config.devServer?.server?.type === 'https';
  if (config.devServer) {
    config.devServer.proxy = proxyConfig({ tls });
  }

  if (targetOptions.target === 'serve' || config.watch) {
    config.plugins.push(
      new BrowserSyncPlugin(
        {
          host: 'localhost',
          port: 9000,
          https: tls,
          // Cấu hình proxy cho BrowserSync
          proxy: {
            // Target chính là webpack-dev-server (cổng 4200)
            target: `http${tls ? 's' : ''}://localhost:4200`,
            ws: false,
          },
          // Giảm thiểu hành vi reload/đồng bộ để không phá WebSocket
          ghostMode: false, // không sync click/scroll/form
          notify: false, // tắt pop-up notify của BrowserSync
          reloadOnRestart: false, // không reload khi BrowserSync restart
          open: false,
          socket: {
            clients: {
              heartbeatTimeout: 60000,
            },
          },
        },
        {
          // Không cho BrowserSync tự reload trang (sẽ phá SockJS/STOMP)
          reload: false,
        },
      ),
    );
  }

  if (config.mode === 'production') {
    config.plugins.push(
      new BundleAnalyzerPlugin({
        analyzerMode: 'static',
        openAnalyzer: false,
        reportFilename: '../../stats.html',
      }),
    );
  }

  const patterns = [
    {
      context: require('swagger-ui-dist').getAbsoluteFSPath(),
      from: '*.{js,css,html,png}',
      to: 'swagger-ui/',
      globOptions: { ignore: ['**/index.html'] },
    },
    {
      from: path.join(path.dirname(require.resolve('axios/package.json')), 'dist/axios.min.js'),
      to: 'swagger-ui/',
    },
    { from: './src/main/webapp/swagger-ui/', to: 'swagger-ui/' },
  ];

  if (patterns.length > 0) {
    config.plugins.push(new CopyWebpackPlugin({ patterns }));
  }

  config.plugins.push(
    new webpack.DefinePlugin({
      __VERSION__: JSON.stringify(environment.__VERSION__),
      SERVER_API_URL: JSON.stringify(environment.SERVER_API_URL),
    }),
  );

  config = merge(config);

  return config;
};
