
import { combineReducers } from 'redux';
import list from './list';
import bulk from './bulk';

const couponsReducer = combineReducers({
  list,
  bulk,
});

export default couponsReducer;
