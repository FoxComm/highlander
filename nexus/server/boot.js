const path = require('path');

process.env.NODE_PATH = `${process.env.NODE_PATH}:${path.resolve('./lib')}`;

require('./env_defaults');

const App = require('./app');

process.title = 'nexus-ui';

const app = new App();
app.start();
