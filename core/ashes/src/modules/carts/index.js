import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import shippingMethods from './shipping-methods';
import paymentMethods from './payment-methods';
import coupons from './coupons';

const cartReducer = combineReducers({
  details,
  list,
  shippingMethods,
  paymentMethods,
  coupons,
});

export default cartReducer;
