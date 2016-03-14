
import KoaApp from 'koa';
import serve from 'koa-static';
import renderReact from '../src/server';
import { makeProxy } from './routes/api';
import zipcodes from './routes/zipcodes';

export default class App extends KoaApp {

  constructor(...args) {
    super(...args);

    this.use(serve('public'))
      .use(makeProxy())
      .use(zipcodes.routes())
      .use(zipcodes.allowedMethods())
      .use(renderReact);
  }

  start() {
    const port = process.env.LISTEN_PORT ? Number(process.env.LISTEN_PORT) : 4040;

    this.listen(port);
    console.info(`Listening on port ${port}`);
  }
}
