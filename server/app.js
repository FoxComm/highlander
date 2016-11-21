
import KoaApp from 'koa';
import serve from 'koa-better-static';
import favicon from 'koa-favicon';
import renderReact from '../src/server';
import { makeApiProxy } from './routes/api';
import { makeElasticProxy } from './routes/elastic';
import zipcodes from './routes/zipcodes';
import loadI18n from './i18n';
import verifyJwt from './verify-jwt';
import onerror from 'koa-onerror';
import moment from 'moment';
import chalk from 'chalk';
import bodyParser from 'koa-bodyparser';
import mandrillRouter from './routes/mandrill';

function timestamp() {
  return moment().format('D MMM H:mm:ss');
}

export default class App extends KoaApp {

  constructor(...args) {
    super(...args);
    onerror(this);

    if (process.env.MAILCHIMP_API_KEY === void 0) {
      throw new Error('Can\'t load MAILCHIMP_API_KEY from environment.');
    }

    this.use(serve('public'))
      .use(favicon('public/favicon.png'))
      .use(makeApiProxy())
      .use(makeElasticProxy())
      .use(bodyParser())
      .use(zipcodes.routes())
      .use(zipcodes.allowedMethods())
      .use(mandrillRouter(process.env.MAILCHIMP_API_KEY))
      .use(verifyJwt)
      .use(loadI18n)
      .use(renderReact);
  }

  start() {
    this.listenPort = process.env.LISTEN_PORT ? Number(process.env.LISTEN_PORT) : 4044;

    this.listen(this.listenPort);
    this.logInfo();
  }

  logInfo() {
    const description = require('../package.json').description;
    /* eslint-disable no-console-log/no-console-log */
    console.log(
      `%s: %s ${chalk.blue('%s')} ${chalk.green('api: %s')} ${chalk.red('development url: http://localhost:%d')}`,
      timestamp(),
      description,
      process.env.NODE_ENV,
      process.env.API_URL,
      this.listenPort
    );
  }
}
