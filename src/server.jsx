
import React from 'react';
import ReactDOM from 'react-dom/server';
import { match, RouterContext } from 'react-router';
import createHistory from 'history/lib/createMemoryHistory';
import { Provider } from 'react-redux';
import jwtDecode from 'jwt-decode';

import makeStore from './store';
import routes from './routes';
import renderPage from '../build/main.html';

export default function *renderReact() {
  const history = createHistory(this.path);

  const jwt = this.cookies.get('JWT');
  let initialState = void 0;

  if (jwt) {
    initialState = {
      auth: {
        jwt,
        user: jwtDecode(jwt),
      },
    };
  }

  const store = makeStore(history, initialState);

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
    this.body = renderPage({
      html: appHtml,
      state: JSON.stringify(store.getState()),
    });
  }
}
