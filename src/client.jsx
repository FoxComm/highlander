
import React from 'react';
import { Router, browserHistory } from 'react-router';
import { Provider } from 'react-redux';
import { render } from 'react-dom';
import makeStore from './store';
import routes from './routes';

const DEBUG = true;

export function renderApp() {
  const store = makeStore(browserHistory, window.__data);

  if (DEBUG) {
    window.store = store;
  }

  render((
    <Provider store={store} key="provider">
      <Router history={browserHistory}>
        {routes}
      </Router>
    </Provider>
  ), document.getElementById('app'));
}
