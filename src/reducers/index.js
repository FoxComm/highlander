'use strict';

import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import giftCards from '../modules/gift-cards';
import giftCardsNew from '../modules/gift-cards-new';
import customers from '../modules/customers';
import { orders } from './orders';

const rootReducer = combineReducers({
  router: routerStateReducer,
  giftCards,
  giftCardsNew,
  customers,
  orders
});

export default rootReducer;