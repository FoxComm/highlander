// LESS styles must be first in output css file
import './less/base.less'; // @todo get rid of

import get from 'lodash/get';
import React from 'react';
import { render } from 'react-dom';
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
import Root from './root';

// global styles
import './css/base.css';

// images
import './favicons';

if (module.hot) {
  module.hot.accept(['./root', './routes', './store'], () => {
    try {
      require('./root');
      require('./routes');
    } catch (e) {
      // pass
    }
    // do nothing, only css reload works
    // because of, for example, https://github.com/pauldijou/redux-act/issues/42
  });
}

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

const currentUser = get(store.getState(), 'user.current');
const needLogin = (!currentUser || !window.tokenOk) && isPathRequiredAuth(location.pathname);

if (needLogin) {
  const loginUri = process.env.BEHIND_NGINX ? '/admin/login' : '/login';

  store.dispatch(push(loginUri));
}

history.listen(location => {
  trackPageView(location.pathname);
});

render(
  <Root store={store} routes={routes} history={history} />,
  document.getElementById('foxcom')
);
