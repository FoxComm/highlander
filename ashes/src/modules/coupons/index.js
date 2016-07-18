
import { combineReducers } from 'redux';
import list from './list';
import bulk from './bulk';
import details from './details';
import couponCodes from './coupon-codes';
import watchers from './watchers';

const couponsReducer = combineReducers({
  list,
  bulk,
  details,
  couponCodes,
  watchers,
});

export default couponsReducer;
