import React from 'react';
import { render } from 'react-dom';
import { Router } from 'react-router';
import { Provider } from 'react-redux';
import { syncHistoryWithStore } from 'react-router-redux';

import { createHistory } from 'history';
import { useRouterHistory } from 'react-router';
import useNamedRoutes from 'use-named-routes';

import configureStore from './store';
import routes from './routes';
import { setHistory } from 'browserHistory';
import { trackPageView, initTracker } from './lib/analytics';

const createBrowserHistory = useNamedRoutes(useRouterHistory(createHistory));

export function start() {
  let history = createBrowserHistory({ routes });

  const initialState = {};
  const store = configureStore(history, initialState);
  history = syncHistoryWithStore(history, store);
  setHistory(history);

  initTracker();
  history.listen(location => {
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
