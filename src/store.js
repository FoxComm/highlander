import { createStore, applyMiddleware, compose } from 'redux';
import thunk from 'redux-thunk';
import rootReducer from './modules';
import { routerMiddleware } from 'react-router-redux';

function useLogger() {
  // only at browser
  return typeof window !== 'undefined';
}

export default function configureStore(history, initialState) {
  const middlewares = [applyMiddleware(thunk), applyMiddleware(routerMiddleware(history))];

  if (useLogger()) {
    const createLogger = require(`redux-logger`);
    const logger = applyMiddleware(createLogger({
      duration: true,
      collapsed: true
    }));
    // logger should be latest always, except devTools()
    middlewares.push(logger);
  }

  return compose(...middlewares)(createStore)(rootReducer, initialState);
}
