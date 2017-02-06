import KoaApp from 'koa';
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
import contactFeedbackRoute from './routes/contact-feedback-route';
import log4js from 'koa-log4';
import path from 'path';
import serve from 'koa-static';

const isProduction = process.env.NODE_ENV === 'production';

function timestamp() {
  return moment().format('D MMM H:mm:ss');
}

/**
 * Set conditional headers to response
 * @param {http.ServerResponse} serverResponse native node server response
 * @param {String} filePath     path to requested file
 * @param {Object} stats        file stats
 */
function setHeaders(serverResponse, filePath, stats) {
  if (isProduction) {
    if (filePath.match(/public\/app.*\.(js|css)/)) {
      this.ctx.response.set('Cache-Control', 'max-age=31536000');
    } else {
      const ims = this.ctx.request.get('If-Modified-Since');
      const ms = Date.parse(ims);

      // https://github.com/ohomer/koa-better-static/blob/master/send.js
      if (ms && Math.floor(ms / 1000) === Math.floor(stats.mtime.getTime() / 1000)) {
        this.ctx.response.status = 304; // not modified
      }
    }
  }
}

export default class App extends KoaApp {

  constructor(...args) {
    super(...args);
    onerror(this);

    if (isProduction &&
      (process.env.MAILCHIMP_API_KEY === undefined ||
      process.env.CONTACT_EMAIL === undefined)) {
      throw new Error(
        'MAILCHIMP_API_KEY and CONTACT_EMAIL variables should be defined in environment.'
      );
    }

    log4js.configure(path.join(`${__dirname}`, '../log4js.json'));

    const context = {};

    this
      .use(function * (next) {
        context.ctx = this;
        yield next;
      })
      .use(serve('public', { setHeaders: setHeaders.bind(context) }))
      .use(favicon('public/favicon.png'))
      .use(log4js.koaLogger(log4js.getLogger('http'), { level: 'auto' }))
      .use(makeApiProxy())
      .use(makeElasticProxy())
      .use(bodyParser())
      .use(zipcodes.routes())
      .use(zipcodes.allowedMethods())
      .use(contactFeedbackRoute(process.env.MAILCHIMP_API_KEY))
      .use(verifyJwt)
      .use(loadI18n)
      .use(renderReact);
  }

  start() {
    this.listenPort = process.env.PORT ? Number(process.env.PORT) : 4044;

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
