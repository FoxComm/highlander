
import { combineReducers } from 'redux';
import list from './list';
import bulk from './bulk';
import details from './details';

const couponsReducer = combineReducers({
  list,
  bulk,
  details,
});

export default couponsReducer;
