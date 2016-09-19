import React from 'react';
import ReactDOM from 'react-dom/server';
import { match, createMemoryHistory, RouterContext } from 'react-router';
import { Provider } from 'react-redux';

import makeStore from './store';
import routes from './routes';
import renderPage from '../build/main.html';

export default function *renderReact() {
  console.log('[renderReact] starting');
  const history = createMemoryHistory();

  const initialState = {};

  const store = makeStore(history, initialState);

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

    console.log('[renderReact] rendering');
    // const appHtml = ReactDOM.renderToString(rootElement);
    const appHtml = yield store.renderToString(ReactDOM, rootElement);
    console.log('[renderReact] done');

    this.body = renderPage({
      html: appHtml,
      state: JSON.stringify(store.getState()),
    });
  }
}
