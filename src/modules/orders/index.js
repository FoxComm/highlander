import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import search from './search';
import shippingMethods from './shipping-methods';
import shippingAddresses from './shipping-addresses';
import paymentMethods from './payment-methods';

const orderReducer = combineReducers({
  details,
  list,
  search,
  shippingMethods,
  shippingAddresses,
  paymentMethods
});

export default orderReducer;
