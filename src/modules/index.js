import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import giftCards from './gift-cards';
import customers from './customers';
import orders from './orders';
import rmas from './rmas';
import notes from './notes';
import products from './products';

const rootReducer = combineReducers({
  router: routerStateReducer,
  giftCards,
  customers,
  orders,
  rmas,
  notes,
  products
});

export default rootReducer;
