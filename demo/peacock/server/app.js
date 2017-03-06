import KoaApp from 'koa';
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
import serve from 'koa-better-static';
import koaMount from 'koa-mount';
import test from './conditional-use';

const isProduction = process.env.NODE_ENV === 'production';

function timestamp() {
  return moment().format('D MMM H:mm:ss');
}

function mount(middleware) {
  if (process.env.URL_PREFIX) {
    return koaMount(process.env.URL_PREFIX, middleware);
  }
  return middleware;
}

function shouldCacheForLongTime(ctx) {
  return isProduction && ctx.path.match(/\/app.*\.(js|css)/);
}

export default class App extends KoaApp {

  constructor(...args) {
    super(...args);
    onerror(this);

    log4js.configure(path.join(`${__dirname}`, '../log4js.json'));

    this
      // serve all static in dev mode through one middleware,
      // enable the second one to add cache headers to app*.js and app*.css
      .use(test(mount(serve('public')), ctx => !shouldCacheForLongTime(ctx)))
      .use(test(mount(serve('public'), { maxage: 31536000 }), shouldCacheForLongTime))
      .use(log4js.koaLogger(log4js.getLogger('http'), { level: 'auto' }))
      .use(makeApiProxy())
      .use(makeElasticProxy())
      .use(bodyParser())
      .use(mount(zipcodes.routes()))
      .use(mount(zipcodes.allowedMethods()))
      .use(mount(contactFeedbackRoute(process.env.MAILCHIMP_API_KEY)))
      .use(verifyJwt)
      .use(loadI18n)
      .use(mount(renderReact));
  }

  start() {
    this.listenPort = process.env.PORT ? Number(process.env.PORT) : 4041;

    this.listen(this.listenPort);
    this.logInfo();
  }

  logInfo() {
    const description = require('../package.json').description;
    console.info(
      `%s: %s ${chalk.blue('%s')} ${chalk.green('api: %s')} ${chalk.red('development url: http://localhost:%d')}`,
      timestamp(),
      description,
      process.env.NODE_ENV,
      process.env.API_URL,
      this.listenPort
    );
  }
}
