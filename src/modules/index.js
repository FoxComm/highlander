import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import giftCards from './gift-cards';
import customers from './customers';
import orders from './orders';
import rmas from './rmas';
import notes from './notes';
import skus from './skus';
import countries from './countries';
import addressForm from './address-form';
import reasons from './reasons';

const rootReducer = combineReducers({
  router: routerStateReducer,
  giftCards,
  customers,
  orders,
  rmas,
  notes,
  skus,
  countries,
  addressForm,
  reasons
});

export default rootReducer;
