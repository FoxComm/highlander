import { createStore, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import rootReducer from './modules';
import { routerMiddleware } from 'react-router-redux';

function useLogger() {
  // only at browser
  return typeof window !== 'undefined';
}

export const baseMiddlewares = [thunk];

export default function configureStore(history, initialState) {
  let middlewares = [...baseMiddlewares, routerMiddleware(history)];

  if (useLogger()) {
    const createLogger = require(`redux-logger`);
    const logger = createLogger({
      duration: true,
      collapsed: true
    });
    // logger should be latest always, except devTools()
    middlewares = [...middlewares, logger];
  }

  return createStore(rootReducer, initialState, applyMiddleware(...middlewares));
}
