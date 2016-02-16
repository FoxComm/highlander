
import React from 'react';
import ReactDOM from 'react-dom/server';
import { match, RouterContext } from 'react-router';
import createHistory from 'history/lib/createMemoryHistory';
import { Provider } from 'react-redux';
import makeStore from './store';
import routes from './routes';

function createPage(html, store) {
  return `
    <!doctype html>
    <html>
      <head>
        <title>StoreFront</title>
      </head>
      <body>
        <link rel="stylesheet" href="./app.css" />
        <div id="app">${html}</div>

        <!-- its a Redux initial data -->
        <script charset="UTF-8">
          window.__data=${JSON.stringify(store.getState())};
        </script>
        <script type="text/javascript" src="./app.js"></script>
        <script type="text/javascript">
          App.renderApp();
        </script>
      </body>
    </html>
  `;
}

export default function *renderReact() {
  const history = createHistory();
  const store = makeStore(history);

  const [redirectLocation, renderProps] = yield match.bind(null, {routes, location: this.path, history });

  if (redirectLocation) {
    this.redirect(redirectLocation.pathname + redirectLocation.search);
  } else if (!renderProps) {
    this.status = 404;
  } else {
    const rootElement = (
      <Provider store={store} key="provider">
        <RouterContext {...renderProps} />
      </Provider>
    );

    const appHtml = yield store.renderToString(ReactDOM, rootElement);
    this.body = createPage(appHtml, store);
  }
}
