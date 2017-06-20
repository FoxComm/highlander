import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import shippingMethods from './shipping-methods';
import paymentMethods from './payment-methods';
import coupons from './coupons';
import bulk from './bulk';

const cartReducer = combineReducers({
  details,
  bulk,
  list,
  shippingMethods,
  paymentMethods,
  coupons,
});

export default cartReducer;
