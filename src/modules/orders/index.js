import { combineReducers } from 'redux';
import lineItems from './line-items';
import details from './details';
import list from './list';
import newOrder from './new-order';
import skuSearch from './sku-search';
import shippingMethods from './shipping-methods';
import shippingAddresses from './shipping-addresses';
import paymentMethods from './payment-methods';

const orderReducer = combineReducers({
  lineItems,
  details,
  list,
  skuSearch,
  shippingMethods,
  shippingAddresses,
  paymentMethods,
  newOrder,
});

export default orderReducer;
