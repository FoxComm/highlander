'use strict';

const fs = require('fs');
const cluster = require('cluster');
const cpus = require('os').cpus().length;
const description = require('./package').description;
const title = require('./package').name;
const clc = require('cli-color');
const moment = require('moment');
const exec = require('child_process').exec;
const path = require('path');

let rev;

try {
  rev = fs.readFileSync(path.resolve(__dirname, '.git-rev'), 'utf8').trim();
} catch (e) {
  rev = 'unknown';
}

process.env.NODE_ENV = process.env.NODE_ENV || 'development';
process.env.API_URL = process.env.API_URL || 'http://localhost';
process.env.NODE_PATH = `${process.env.NODE_PATH}:${path.resolve('./lib')}`;
process.env.GIT_REVISION = rev;

let forks = process.env.NODE_ENV === 'production' ? cpus : 1;

if (cluster.isWorker) {
  return require('./server').init()
    .catch(function(err) {
      console.error(err.stack);
    });
}

process.title = `m-${title}`;

function timestamp() {
  return moment().format('D MMM H:mm:ss');
}

function disconnectWorker(worker) {
  worker.disconnect();
  let timeout = setTimeout(function() {
    worker.kill();
  }, 10 * 1000);
  worker.on('disconnect', function() {
    console.log(`${timestamp()}: ${description} worker ${worker.id} shutdown.`);
    clearTimeout(timeout);
  });
}

process.on('SIGUSR2', function() {
  console.log(`${timestamp()}: Restarting ${title} workers`);
  delete require.cache[require.resolve("./server")];

  for (let id in cluster.workers) {
    let worker = cluster.workers[id];
    if (worker.state === 'listening') {
      worker.send('shutdown');
      disconnectWorker(worker);
      cluster.fork();
    }
  }
});

cluster.on('exit', function(worker, code) {
  if (code !== 0) {
    console.log(`${timestamp()}: ${description} worker ${worker.id} died. restart...`);
    cluster.fork();
  }
});

cluster.on('listening', function(worker, address) {
  let cmd = `git log -1 --format='%h' -- ${__dirname}`;
  exec(cmd, function(err, stdout) {
    let sha = stdout.toString().trim();
    let basePath = process.env.ON_SERVER ? '/admin' : '';
    let args = [
      `%s: %s (${clc.blackBright('%s')}) ${clc.blue('%s')} ${clc.green('api: %s')} ${clc.red('development url: http://localhost:%d%s')}`,
      timestamp(),
      description,
      sha,
      process.env.NODE_ENV,
      process.env.API_URL,
      address.port,
      basePath,
    ];
    console.log.apply(this, args);
  });
});

while (forks--) cluster.fork();
