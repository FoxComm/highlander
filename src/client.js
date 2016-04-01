import React from 'react';
import { render } from 'react-dom';
import { reduxReactRouter, ReduxRouter } from 'redux-router';
import { createHistory as _createHistory } from 'history';

import routes from './routes';
import { Provider } from 'react-redux';
import configureStore from './store';
import { addRouteLookupForHistory } from './route-helpers';

const createHistory = addRouteLookupForHistory(_createHistory, routes);


export function start() {
  const initialState = {};
  const store = configureStore(reduxReactRouter, routes, createHistory, initialState);

  render(
    <Provider store={store} key="provider">
      <ReduxRouter routes={routes} />
    </Provider>,
    document.getElementById('foxcom')
  );
}
