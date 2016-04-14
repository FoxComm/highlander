
import { combineReducers } from 'redux';
import list from './list';
import bulk from './bulk';
import details from './details';
import couponCodes from './coupon-codes';

const couponsReducer = combineReducers({
  list,
  bulk,
  details,
  couponCodes,
});

export default couponsReducer;
