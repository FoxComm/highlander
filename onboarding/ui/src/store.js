import compact from 'lodash/compact';
import { createStore, applyMiddleware as clientApplyMiddleware } from 'redux';
import { routerMiddleware } from 'react-router-redux';
import thunk from 'redux-thunk';
import createLogger from 'redux-logger';

import isServer from './utils/isServer';
import serverApplyMiddleware from './middlewares/wait';
import redirect from './middlewares/redirect';
import rootReducer from './core/modules/index';


function initLogger(): ?Function {
  return isServer ? null : createLogger({
    duration: true,
    collapsed: true,
    diff: true,
    predicate: (getState, action) => action.type.indexOf('redux-form/') !== 0,
  });
}

export default function makeStore(history, initialState = void 0, app) {
  const applyMiddleware = isServer ? serverApplyMiddleware : clientApplyMiddleware;

  return createStore(
    rootReducer,
    initialState,
    applyMiddleware(...compact([routerMiddleware(history), thunk, redirect(app), initLogger()]))
  );
}
