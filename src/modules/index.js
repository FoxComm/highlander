import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import giftCards from './gift-cards';
import customers from './customers';
import orders from './orders';
import rmas from './rmas';
import notes from './notes';
import countries from './countries';
import addressForm from './address-form';
import addresses from './addresses';

const rootReducer = combineReducers({
  router: routerStateReducer,
  giftCards,
  customers,
  orders,
  rmas,
  notes,
  countries,
  addressForm,
  addresses,
});

export default rootReducer;
