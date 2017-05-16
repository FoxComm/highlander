/* @flow weak */

import _ from 'lodash';
import { createStore, applyMiddleware as clientApplyMiddleware } from 'redux';
import { createLogger } from 'redux-logger';
import rootReducer from './modules/index';
import { routerMiddleware } from 'react-router-redux';

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

  const store = createStore(
    rootReducer,
    initialState,
    _.flow(applyMiddleware(middleware), applyMiddleware(logger))
  ); // eslint-disable-line spaced-comment

  /* ::`*/ if (module.onReload) {
    module.onReload(() => {
      const nextReducer = require('./modules');
      store.replaceReducer(nextReducer.default || nextReducer);

      return true;
    });
  } // eslint-disable-line spaced-comment
  /* ::`;*/ return store;
}
