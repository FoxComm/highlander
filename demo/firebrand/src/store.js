
import _ from 'lodash';
import { createStore, applyMiddleware as clientApplyMiddleware } from 'redux';
import { syncHistory } from 'react-router-redux';
// @TODO: drop redux-isomorphic-render from client bundle
import { default as serverApplyMiddleware } from 'redux-isomorphic-render';
import logger from 'redux-diff-logger';
import rootReducer from 'modules/index';
import { api } from 'lib/api';

const isServer = typeof self == 'undefined';

function thunkMiddleware({dispatch, getState}) {
  return function (next) {
    return function (action) {
      if (typeof action === 'function') {
        const jwt = _.get(getState(), 'auth.jwt');
        const headers = {JWT: jwt};
        const {authHeader} = getState();
        if (authHeader) {
          headers.Authorization = authHeader;
        }
        return action(dispatch, getState, api.addHeaders(headers));
      }
      return next(action);
    };
  };
}


export default function makeStore(history, initialState = void 0) {
  const reduxRouterMiddleware = syncHistory(history);
  const applyMiddleware = isServer ? serverApplyMiddleware : clientApplyMiddleware;

  const store = createStore(
    rootReducer,
    initialState,
    _.flow(..._.compact([
      !isServer ? applyMiddleware(logger) : null,
      applyMiddleware(reduxRouterMiddleware),
      applyMiddleware(thunkMiddleware),
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
