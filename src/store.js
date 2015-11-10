import { createStore, applyMiddleware, compose } from 'redux';
import thunk from 'redux-thunk';
import createLogger from 'redux-logger';
import rootReducer from './modules';

export default function configureStore(reduxReactRouter, routes, createHistory, initialState) {
  let finalCreateStore = compose(
    applyMiddleware(thunk),
    reduxReactRouter({ routes, createHistory }),
    applyMiddleware(createLogger({
      logger: console
    })) // logger should be latest always, except devTools()
  )(createStore);

  return finalCreateStore(rootReducer, initialState);
}
