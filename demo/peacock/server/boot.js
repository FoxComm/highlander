
const path = require('path');
const { fork } = require('child_process');

require('./setup_env');

const child = fork(path.join(__dirname, 'app.js'));

function killChild() {
  try {
    process.kill(child.pid);
  } catch (e) {
    if (e.code != 'ESRCH') throw e;
  }
}

process.on('exit', killChild);
process.on('SIGINT', killChild);
process.on('SIGTERM', killChild);
process.on('uncaughtException', killChild);
