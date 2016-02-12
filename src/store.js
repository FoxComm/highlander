
import _ from 'lodash';
import { createStore } from 'redux';
import { syncHistory } from 'react-router-redux'
import applyMiddleware from 'redux-wait';
import thunk from 'redux-thunk';
import rootReducer from './modules';

export default function makeStore(history, initialState = void 0) {

  const reduxRouterMiddleware = syncHistory(history);

  return createStore(
    rootReducer,
    initialState,
    _.flow(
      applyMiddleware(reduxRouterMiddleware),
      applyMiddleware(thunk)
    )
  );
}
