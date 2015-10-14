'use strict';

import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import giftCards from '../modules/gift-cards';
import { orders } from './orders';

const rootReducer = combineReducers({
  router: routerStateReducer,
  giftCards,
  orders
});

export default rootReducer;