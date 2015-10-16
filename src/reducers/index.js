'use strict';

import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import giftCards from '../modules/gift-cards';
import customers from '../modules/customers';
import orders from '../modules/orders';
import order from '../modules/order'; 

const rootReducer = combineReducers({
  router: routerStateReducer,
  giftCards,
  customers,
  orders,
  order
});

export default rootReducer;