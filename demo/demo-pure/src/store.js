
import _ from 'lodash';
import { createStore, applyMiddleware as clientApplyMiddleware } from 'redux';
import { syncHistory } from 'react-router-redux';
// @TODO: drop redux-isomorphic-render from client bundle
import { default as serverApplyMiddleware } from 'redux-isomorphic-render';
import createLogger from 'redux-logger';
import rootReducer from 'modules/index';
import { api } from 'lib/api';

const isServer = typeof self == 'undefined';
const isDebug = process.env.NODE_ENV != 'production';

export function thunkMiddleware({dispatch, getState}) {
  return function (next) {
    return function (action) {
      if (typeof action === 'function') {
        const jwt = _.get(getState(), 'auth.jwt');
        api.addAuth(jwt);
        const {authHeader} = getState();
        if (authHeader) {
          api.addHeaders({
            Authorization: authHeader,
          });
        }
        return action(dispatch, getState, api);
      }
      return next(action);
    };
  };
}

export default function makeStore(history, initialState = void 0) {
  const reduxRouterMiddleware = syncHistory(history);
  const applyMiddleware = isServer ? serverApplyMiddleware : clientApplyMiddleware;

  const logger = createLogger({
    duration: true,
    collapsed: true,
  });

  const store = createStore(
    rootReducer,
    initialState,
    _.flow(..._.compact([
      !isServer && isDebug ? applyMiddleware(logger) : null,
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
