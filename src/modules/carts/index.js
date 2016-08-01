import { combineReducers } from 'redux';
import details from './details';
import skuSearch from './sku-search';
import shippingMethods from './shipping-methods';
import paymentMethods from './payment-methods';
import coupons from './coupons';

const cartReducer = combineReducers({
  details,
  skuSearch,
  shippingMethods,
  paymentMethods,
  coupons,
});

export default cartReducer;
