import React from 'react';
import ReactDOM from 'react-dom/server';
import { match, createMemoryHistory, RouterContext } from 'react-router';
import { Provider } from 'react-redux';
import { syncHistoryWithStore } from 'react-router-redux';

import makeStore from './store';
import routes from './routes';
import renderPage from '../build/main.html';

export default function *renderReact() {
  const memoryHistory = createMemoryHistory();

  const initialState = {};

  const store = makeStore(memoryHistory, initialState, this);
  const history = syncHistoryWithStore(memoryHistory, store);

  const [redirectLocation, renderProps] = yield match.bind(null, { routes, location: this.url, history });

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

    // const appHtml = ReactDOM.renderToString(rootElement);
    const appHtml = yield store.renderToString(ReactDOM, rootElement);

    this.body = renderPage({
      html: appHtml,
      ashesUrl: process.env.ASHES_URL,
      state: JSON.stringify(store.getState()),
    });
  }
}
