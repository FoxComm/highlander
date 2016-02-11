
import _ from 'lodash';
import { reducer as reduxAsyncConnect } from 'redux-async-connect';
import { createStore, combineReducers, applyMiddleware } from 'redux';
import { routeReducer, syncHistory } from 'react-router-redux'
import reduceReducers from 'reduce-reducers';
import rootReducer from './modules';

export default function makeStore(history, initialState = void 0) {
  const reduxRouterMiddleware = syncHistory(history);

  const supportReducers = combineReducers({
    reduxAsyncConnect,
    routing: routeReducer
  });

  return createStore(
    reduceReducers(supportReducers, rootReducer),
    initialState,
    _.flow(
      applyMiddleware(reduxRouterMiddleware)
    )
  );
}
