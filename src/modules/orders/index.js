import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import shippingMethods from './shipping-methods';

const orderReducer = combineReducers({
  details,
  list,
  shippingMethods
});

export default orderReducer;
