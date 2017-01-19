#!/usr/bin/env node

require('colors');
const path = require('path');
const Koa = require('koa');
const makeProxy = require('./proxy');
const body = require('koa-better-body');

const argv = require('yargs')
  .usage('Usage: $0 [options]')
  .default('port', 6144)
  .boolean('ignore-tls')
  .default('ignore-tls', true)
  .example('$0 -c ./kangaroos-cache -t https://kangaroos.foxcommerce.com', 'Make a proxy server which save successful request to kangaroos-cache dir')
  .alias('c', 'cache-dir')
  .alias('t', 'target-host')
  .nargs('c', 1)
  .nargs('t', 1)
  .describe('c', 'Provide caching dir for successful requests')
  .describe('t', 'Target host for doing requests')
  .describe('port', 'Listening port')
  .demandOption(['c', 't'])
  .coerce(['c'], path.resolve)
  .help('h')
  .alias('h', 'help')
  .epilog('copyright FoxCommerce team')
  .argv;

if (argv.ignoreTls) {
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = 0;
  console.log('ignored tls errors');
}

const app = new Koa();

const proxy = makeProxy({
  host: argv.t,
  cacheDir: argv.c,
});

app.use(body());
app.use(function*() {
  yield proxy;
});

app.listen(argv.port);
console.log(`Proxy listening on ${argv.port}`);
