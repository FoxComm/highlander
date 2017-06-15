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
process.env.GIT_REVISION = rev;
process.env.PORT = process.env.PORT || 4000;

const args = [
  `${clc.blackBright('NODE_ENV:')} ${clc.blue('%s')}, ${clc.blackBright('API_URL:')}\
  ${clc.green('%s')}, ${clc.red('url: http://localhost:%d')}`,
  process.env.NODE_ENV,
  process.env.API_URL || 'Not defined',
  process.env.PORT,
];
console.log.apply(this, args); // eslint-disable-line no-console

app.init()
  .catch(function (err) {
    console.error(err.stack);
  });
