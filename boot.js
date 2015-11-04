'use strict';

const
  cluster     = require('cluster'),
  cpus        = require('os').cpus().length,
  description = require('./package').description,
  title       = require('./package').name,
  clc         = require('cli-color'),
  moment      = require('moment'),
  exec        = require('child_process').exec;

process.env.NODE_ENV = process.env.NODE_ENV || 'development';
process.env.PHOENIX_URL = process.env.PHOENIX_URL || 'http://localhost:9090';

let forks = process.env.NODE_ENV === 'production' ? cpus : 1;

if (cluster.isWorker) {
  return require('./server').init()
    .catch(function(err){
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
  exec(cmd, function(err, stdout, stderr) {
    let sha = stdout.toString().trim();
    let args = [
      `%s: %s (${clc.blackBright('%s')}) ${clc.blue('%s')} ${clc.green('phoenix: %s')} ${clc.red('%d')}`,
      timestamp(),
      description,
      sha,
      process.env.NODE_ENV,
      process.env.PHOENIX_URL,
      address.port
    ]
    console.log.apply(this, args);
  });
});

while (forks--) cluster.fork();
