
require('babel-register');
require('../src/postcss').installHook();

const App = require('./app').default;

const app = new App();
app.start();
