
import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import shippingMethods from './shipping-methods';
import shippingAddresses from './shipping-addresses';

const orderReducer = combineReducers({
  details,
  list,
  shippingMethods,
  shippingAddresses,
});

export default orderReducer;
