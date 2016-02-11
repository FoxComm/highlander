
import React from 'react';
import { renderToString } from 'react-dom/server'
import { match, RoutingContext } from 'react-router'
import { ReduxAsyncConnect, loadOnServer, reducer as reduxAsyncConnect } from 'redux-async-connect'
import createHistory from 'history/lib/createMemoryHistory';
import {Provider} from 'react-redux';
import makeStore from './store';
import routes from './routes';

export default function *renderReact(next) {
  const history = createHistory();
  const store = makeStore(history);

  const [redirectLocation, renderProps] = yield match.bind(null, {routes, location: this.path, history });

  if (redirectLocation) {
    this.redirect(redirectLocation.pathname + redirectLocation.search);
  } else if (!renderProps) {
    this.status = 404;
  } else {
    yield loadOnServer(renderProps, store);

    const appHtml = renderToString(
      <Provider store={store} key="provider">
        <ReduxAsyncConnect {...renderProps} />
      </Provider>
    );

    this.body = createPage(appHtml, store);
  }
};

function createPage(html, store) {
  return `
    <!doctype html>
    <html>
      <body>
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
