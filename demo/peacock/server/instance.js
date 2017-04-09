
require('../src/postcss.config').installHook();

const App = require('./app');

process.title = process.env.STOREFRONT_NAME || 'fox-storefront';

const app = new App();
app.start();
