'use strict';

import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import giftCards from './gift-cards';
import customers from './customers';
import orders from './orders';

const rootReducer = combineReducers({
  router: routerStateReducer,
  giftCards,
  customers,
  orders
});

export default rootReducer;
