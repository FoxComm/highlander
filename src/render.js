import React from 'react';
import { renderToString } from 'react-dom/server';
import { ReduxRouter } from 'redux-router';
import {reduxReactRouter as serverReduxReactRouter, match as _match} from 'redux-router/server';
import { createMemoryHistory as _createMemoryHistory } from 'history';

import routes from './routes';
import { Provider } from 'react-redux';
import configureStore from './store';
import { addRouteLookupForHistory } from './route-helpers';

const createMemoryHistory = addRouteLookupForHistory(_createMemoryHistory, routes);

function match(store, url, callback) {
  store.dispatch(_match(url, callback));
}

export function * renderReact(next) {
  const initialState = {};
  const store = configureStore(serverReduxReactRouter, routes, createMemoryHistory, initialState);

  let [redirectLocation, routerState] = yield match.bind(null, store, this.path);

  if (redirectLocation) {
    this.redirect(redirectLocation.pathname + redirectLocation.search);
  } else if (!routerState) {
    this.status = 404;
  } else {
    this.state.html = renderToString(
      <Provider store={store} key="provider">
        <ReduxRouter/>
      </Provider>
    );

    yield next;
  }
}
