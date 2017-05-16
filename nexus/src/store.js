/* @flow weak */

import _ from 'lodash';
import { createStore, applyMiddleware as clientApplyMiddleware } from 'redux';
import { syncHistory } from 'react-router-redux';
import { createLogger } from 'redux-logger';
import rootReducer from './modules/index';
import {
  ConnectedRouter,
  routerReducer,
  routerMiddleware,
  push,
} from 'react-router-redux';

const isServer = typeof self == 'undefined';
const isDebug = process.env.NODE_ENV != 'production';

export default function makeStore(
  history: Object,
  initialState: ?Object = void 0
) {
  const applyMiddleware = clientApplyMiddleware;

  const logger = createLogger({
    duration: true,
    collapsed: true,
  });

  const middleware = routerMiddleware(history);

  const store = createStore(rootReducer, applyMiddleware(middleware));

  /*::`*/
  if (module.onReload) {
    module.onReload(() => {
      const nextReducer = require('./modules');
      store.replaceReducer(nextReducer.default || nextReducer);

      return true;
    });
  }
  /*::`;*/

  return store;
}
