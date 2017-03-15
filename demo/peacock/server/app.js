const KoaApp = require('koa');
const { makeApiProxy } = require('./routes/api');
const { makeElasticProxy } = require('./routes/elastic');
const zipcodes = require('./routes/zipcodes');
const loadI18n = require('./i18n');
const verifyJwt = require('./verify-jwt');
const onerror = require('koa-onerror');
const moment = require('moment');
const chalk = require('chalk');
const bodyParser = require('koa-bodyparser');
const contactFeedbackRoute = require('./routes/contact-feedback-route');
const log4js = require('koa-log4');
const path = require('path');
const serve = require('koa-better-static');
const koaMount = require('koa-mount');
const test = require('./conditional-use');
const { renderReact } = require('../lib/server');

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

class App extends KoaApp {

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

module.exports = App;
