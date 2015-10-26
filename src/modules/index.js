'use strict';

import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import giftCards from './gift-cards';
import customers from './customers';
import orders from './orders';
import order from './order';
import notes from './notes';

const rootReducer = combineReducers({
  router: routerStateReducer,
  giftCards,
  customers,
  orders,
  order,
  notes
});

export default rootReducer;
