
import _ from 'lodash';
import { createStore, applyMiddleware as clientApplyMiddleware } from 'redux';
import { syncHistory } from 'react-router-redux';
// @TODO: drop redux-isomorphic-render from client bundle
import { default as serverApplyMiddleware } from 'redux-isomorphic-render';
import logger from 'redux-diff-logger';
import thunk from 'redux-thunk';
import rootReducer from 'modules/index';

const isServer = typeof self == 'undefined';

export default function makeStore(history, initialState = void 0) {
  const reduxRouterMiddleware = syncHistory(history);
  const applyMiddleware = isServer ? serverApplyMiddleware : clientApplyMiddleware;

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
