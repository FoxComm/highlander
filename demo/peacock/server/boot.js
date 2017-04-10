
const path = require('path');
const { fork } = require('child_process');

require('./setup_env');

fork(path.join(__dirname, 'app.js'));
