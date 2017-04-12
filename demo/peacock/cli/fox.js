#!/usr/bin/env node
const { spawn } = require('child_process');

const argv = require('yargs')
  .usage('$0 <cmd> [args]')
  .command('init', 'install node.js dependencies for storefront')
  .epilog('FoxCommerce team')
  .argv;

const command = argv._[0];

const deps = [
  'babel-runtime', 'react'
];

function runYarn(args, cb = () => {}) {
  let child = spawn('yarn',
    args,
    {
      shell: true,
      detached: true,
      stdio: 'inherit',
    }
  ).on('close', (code) => {
    child = null;
    if (code != 0) {
      cb(new Error(`"yarn run ${name}" process exited with code ${code}`));
    } else {
      cb();
    }
  }).on('error', (err) => {
    child = null;
    cb(err);
  });

  process.on('exit', () => {
    if (child) process.kill(-child.pid);
  });

  return child;
}

function init() {
  runYarn(['add', ...deps]);
}

switch (command) {
  case 'init':
    init();
    break;
}
