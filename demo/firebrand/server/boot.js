
const path = require('path');

process.env.NODE_PATH = `${process.env.NODE_PATH}:${path.resolve('./src/core')}`;

require('babel-register')();
require('../src/postcss').installHook();
require('./env_defaults');

const App = require('./app').default;

process.title = 'm-firebird';

const app = new App();
app.start();
