
import _ from 'lodash';
import { createStore, applyMiddleware as clientApplyMiddleware } from 'redux';
import { syncHistory } from 'react-router-redux';
// @TODO: drop redux-isomorphic-render from client bundle
import { default as serverApplyMiddleware } from 'redux-isomorphic-render';
import logger from 'redux-diff-logger';
import rootReducer from 'modules/index';
import redirectMiddleware from 'middlewares/redirect';
import { api } from 'lib/api';

const isServer = typeof self == 'undefined';

function thunkMiddleware({dispatch, getState}) {
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

export default function makeStore(history, initialState = void 0, app = void 0) {
  const reduxRouterMiddleware = syncHistory(history);
  const applyMiddleware = isServer ? serverApplyMiddleware : clientApplyMiddleware;

  const store = createStore(
    rootReducer,
    initialState,
    _.flow(..._.compact([
      !isServer ? applyMiddleware(logger) : null,
      isServer ? applyMiddleware(redirectMiddleware(app)) : null,
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
