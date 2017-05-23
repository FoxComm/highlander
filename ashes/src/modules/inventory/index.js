import { combineReducers } from 'redux';
import list from './list';
import warehouses from './warehouses';
import transactions from './transactions';
import bulk from './bulk';

const customerReducer = combineReducers({
  list,
  bulk,
  warehouses,
  transactions,
});

export default customerReducer;
