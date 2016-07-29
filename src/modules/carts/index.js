import { combineReducers } from 'redux';
import details from './details';
// import bulk from './bulk';
// import watchers from './watchers';
// import list from './list';
// import newOrder from './new-order';
import skuSearch from './sku-search';
import shippingMethods from './shipping-methods';
// import paymentMethods from './payment-methods';
import coupons from './coupons';
// import discounts from './discounts';
// import shipments from './shipments';
// import carriers from './carriers';
// import shipmentMethods from './shipment-methods';

const cartReducer = combineReducers({
  details,
  // bulk,
  // watchers,
  // list,
  skuSearch,
  shippingMethods,
  // paymentMethods,
  // newOrder,
  coupons,
  // discounts,
  // shipments,
  // carriers,
  // shipmentMethods,
});

export default cartReducer;
