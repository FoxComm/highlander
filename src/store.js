
import _ from 'lodash';
import { createStore } from 'redux';
import { syncHistory } from 'react-router-redux';
import applyMiddleware from 'redux-wait';
import logger from 'redux-diff-logger';
import thunk from 'redux-thunk';
import rootReducer from 'modules/index';

const isServer = typeof self == 'undefined';

export default function makeStore(history, initialState = void 0) {
  const reduxRouterMiddleware = syncHistory(history);

  const store = createStore(
    rootReducer,
    initialState,
    _.flow(..._.compact([
      !isServer ? applyMiddleware(logger) : null,
      applyMiddleware(reduxRouterMiddleware),
      applyMiddleware(thunk),
    ]))
  );

  if (module.onReload) {
    module.onReload(() => {
      const nextReducer = require('modules');
      store.replaceReducer(nextReducer.default || nextReducer);

      return true;
    });
  }

  return store;
}
