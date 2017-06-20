import KoaApp from 'koa';
import serve from 'koa-better-static';
import onerror from 'koa-onerror';

import renderReact from '../src/server';
import makeApiProxy from './routes/api';
import s3sign from './s3sign';
import token from './token';

export default class App extends KoaApp {

  constructor(...args) {
    super(...args);
    onerror(this);

    this.use(serve('public'))
      .use(makeApiProxy())
      .use(s3sign)
      .use(token)
      .use(renderReact);
  }

  start() {
    const port = process.env.PORT ? Number(process.env.PORT) : 4042;

    this.listen(port);
    console.info(`Listening on port ${port}`);
  }
}
