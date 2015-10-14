'use strict';

import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import { orders } from './orders';

const rootReducer = combineReducers({
  router: routerStateReducer,
  orders
});

export default rootReducer;