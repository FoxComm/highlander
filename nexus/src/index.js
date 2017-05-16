/* @flow */

import React from 'react';
import { render } from 'react-dom';
import makeStore from './store';
import makeRoutes from './routes';
import createHistory from 'history/createBrowserHistory';
import { ConnectedRouter } from 'react-router-redux';
import { Provider } from 'react-redux';

function renderApp() {
  const history = createHistory();
  const store = makeStore(history, window.__data);
  const routes = makeRoutes(store);

  render(
    <Provider store={store} key="provider">
      <ConnectedRouter history={history}>
        {routes}
      </ConnectedRouter>
    </Provider>,
    document.getElementById('app')
  );
}

renderApp();
