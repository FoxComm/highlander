import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import skuSearch from './sku-search';
import shippingMethods from './shipping-methods';
import paymentMethods from './payment-methods';
import coupons from './coupons';

const cartReducer = combineReducers({
  details,
  list,
  skuSearch,
  shippingMethods,
  paymentMethods,
  coupons,
});

export default cartReducer;
