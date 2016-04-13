
const path = require('path');

require('babel-register')({
  ignore: path.join(process.cwd(), 'node_modules'),
});
require('../src/postcss').installHook();
require('./env_defaults');

const App = require('./app').default;

process.title = 'm-firebird';

const app = new App();
app.start();
