import compact from 'lodash/compact';
import { createStore, applyMiddleware as clientApplyMiddleware } from 'redux';
import { routerMiddleware } from 'react-router-redux';
import { default as serverApplyMiddleware } from 'redux-isomorphic-render';
import thunk from 'redux-thunk';
import createLogger from 'redux-logger';

import rootReducer from './core/modules/index';

const isServer = typeof self == 'undefined';

function initLogger(): ?Function {
  return isServer ? null : createLogger({
    duration: true,
    collapsed: true,
    diff: true,
  });
}

export default function makeStore(history, initialState = void 0) {
  const applyMiddleware = clientApplyMiddleware;

  return createStore(
    rootReducer,
    initialState,
    applyMiddleware(...compact([routerMiddleware(history), thunk, initLogger()]))
  );
}
