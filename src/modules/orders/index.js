import { combineReducers } from 'redux';
import details from './details';
import bulk from './bulk';
import watchers from './watchers';
import list from './list';
import newOrder from './new-order';
import paymentMethods from './payment-methods';
import coupons from './coupons';
import discounts from './discounts';
import shipments from './shipments';
import carriers from './carriers';
import shipmentMethods from './shipment-methods';

const orderReducer = combineReducers({
  details,
  bulk,
  watchers,
  list,
  paymentMethods,
  newOrder,
  coupons,
  discounts,
  shipments,
  carriers,
  shipmentMethods,
});

export default orderReducer;
