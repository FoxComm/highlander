import get from 'lodash/get';
import React from 'react';
import { render } from 'react-dom';
import { Router } from 'react-router';
import { Provider } from 'react-redux';
import { syncHistoryWithStore, push } from 'react-router-redux';

import { createHistory } from 'history';
import { useRouterHistory } from 'react-router';
import useNamedRoutes from 'use-named-routes';

import configureStore from './store';
import makeRoutes from './routes';
import { setHistory } from 'browserHistory';
import { trackPageView, initTracker } from './lib/analytics';
import { getJWT } from 'lib/claims';
import { isPathRequiredAuth } from './route-rules';

const createBrowserHistory = useNamedRoutes(useRouterHistory(createHistory));

// get jwt provided by server and writes it to localStorage
export function syncJWTFromServer() {
  if (JWTString) {
    localStorage.setItem('jwt', JWTString);
  } else {
    localStorage.removeItem('jwt');
  }
}

syncJWTFromServer();

export function start() {
  const routes = makeRoutes();
  let history = createBrowserHistory({ routes });

  const initialState = {
    user: {
      current: getJWT(),
    },
  };
  const store = configureStore(history, initialState);
  history = syncHistoryWithStore(history, store);
  setHistory(history);

  if (process.env.NODE_ENV != 'production') {
    window.store = store;
  }

  initTracker();
  history.listen(location => {
    // reset title in order to have default title if page will not set own one
    document.title = 'FoxCommerce';
    trackPageView(location.pathname);

    if (!get(store.getState(), 'user.current') && isPathRequiredAuth(location.pathname)) {
      store.dispatch(push('/login'));
    }
  });

  render(
    <Provider store={store} routes={routes} key="provider">
      <Router history={history}>
        {routes}
      </Router>
    </Provider>,
    document.getElementById('foxcom')
  );
}
