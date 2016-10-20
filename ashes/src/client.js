import React from 'react';
import { render } from 'react-dom';
import { Router } from 'react-router';
import { Provider } from 'react-redux';
import { syncHistoryWithStore } from 'react-router-redux';

import { createHistory } from 'history';
import { useRouterHistory } from 'react-router';
import useNamedRoutes from 'use-named-routes';

import configureStore from './store';
import makeRoutes from './routes';
import { setHistory } from 'browserHistory';
import { trackPageView, initTracker } from './lib/analytics';
import { getJWT } from 'lib/claims';

const createBrowserHistory = useNamedRoutes(useRouterHistory(createHistory));

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

  initTracker();
  history.listen(location => {
    // reset title in order to have default title if page will not set own one
    document.title = 'FoxCommerce';
    trackPageView(location.pathname);
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
