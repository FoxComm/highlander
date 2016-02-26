
import KoaApp from 'koa';
import serve from 'koa-static';
import renderReact from '../src/server';

export default class App extends KoaApp {

  constructor(...args) {
    super(...args);

    this.use(serve('public'));
    this.use(renderReact);
  }

  start() {
    const port = process.env.LISTEN_PORT ? Number(process.env.LISTEN_PORT) : 4040;

    this.listen(port);
    console.log(`Listening on port ${port}`);
  }
}
