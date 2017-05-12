'use strict';

const fs = require('fs');
const clc = require('cli-color');
const path = require('path');
const app = require('./app');

let rev;

try {
  rev = fs.readFileSync(path.resolve(__dirname, '.git-rev'), 'utf8').trim();
} catch (e) {
  rev = 'unknown';
}

// env Defaults
process.env.NODE_ENV = process.env.NODE_ENV || 'development';
process.env.API_URL = process.env.API_URL || 'http://localhost';
process.env.GIT_REVISION = rev;
process.env.PORT = process.env.PORT || 4000;

const basePath = process.env.BEHIND_NGINX ? '/admin' : '';
const args = [
  `${clc.blackBright('NODE_ENV:')} ${clc.blue('%s')}, ${clc.blackBright('API_URL:')} ${clc.green('%s')}, ${clc.red('url: http://localhost:%d%s')}`,
  process.env.NODE_ENV,
  process.env.API_URL,
  process.env.PORT,
  basePath,
];
console.log.apply(this, args); // eslint-disable-line no-console

app.init()
  .catch(function (err) {
    console.error(err.stack);
  });
