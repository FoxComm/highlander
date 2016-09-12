import KoaApp from 'koa';
import serve from 'koa-static';
import renderReact from '../src/server';
import { makeApiProxy } from './routes/api';
import onerror from 'koa-onerror';

export default class App extends KoaApp {

  constructor(...args) {
    super(...args);
    onerror(this);

    this.use(serve('public'))
      .use(makeApiProxy())
      .use(renderReact);
  }

  start() {
    const port = process.env.LISTEN_PORT ? Number(process.env.LISTEN_PORT) : 4042;

    this.listen(port);
    console.info(`Listening on port ${port}`);
  }
}
