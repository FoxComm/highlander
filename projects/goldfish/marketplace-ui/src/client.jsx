import React from 'react';
import { Router, browserHistory, applyRouterMiddleware } from 'react-router';
import { Provider } from 'react-redux';
import { render } from 'react-dom';
import makeStore from './store';
import routes from './routes';

const DEBUG = true;

export function renderApp() {
  const store = makeStore(browserHistory, window.initialState);

  if (DEBUG) {
    window.store = store;
  }

  render((
    <Provider store={store} key="provider">
      <Router history={browserHistory} render={applyRouterMiddleware()}>
        {routes}
      </Router>
    </Provider>
  ), document.getElementById('app'));
}
