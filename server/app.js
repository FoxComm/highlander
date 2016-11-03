
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

export default class App extends KoaApp {

  constructor(...args) {
    super(...args);
    onerror(this);

    this.use(serve('public'))
      .use(favicon('public/favicon.png'))
      .use(makeApiProxy())
      .use(makeElasticProxy())
      .use(zipcodes.routes())
      .use(zipcodes.allowedMethods())
      .use(verifyJwt)
      .use(loadI18n)
      .use(renderReact);
  }

  start() {
    const port = process.env.LISTEN_PORT ? Number(process.env.LISTEN_PORT) : 4044;

    this.listen(port);
    console.info(`Listening on port ${port}`);
  }
}
