require('babel-register')();
require('../src/postcss').installHook();

process.env.NODE_ENV = process.env.NODE_ENV || 'development';
process.env.API_URL = process.env.API_URL || 'http://localhost';

const App = require('./app').default;

process.title = 'm-onboarding';

const app = new App();
app.start();
