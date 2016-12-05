
import KoaApp from 'koa';
import serve from 'koa-better-static';
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
      .use(makeApiProxy())
      .use(makeElasticProxy())
      .use(zipcodes.routes())
      .use(zipcodes.allowedMethods())
      .use(verifyJwt)
      .use(loadI18n)
      .use(renderReact);
  }

  start() {
    const port = process.env.PORT ? Number(process.env.PORT) : 4041;

    this.listen(port);
    console.info(`Listening on port ${port}`);
  }
}
