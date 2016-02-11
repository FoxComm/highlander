
require('babel-register');
const App = require('./app').default;

const app = new App();
app.start();
