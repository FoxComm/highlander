import { combineReducers } from 'redux';
import list from './list';
import warehouses from './warehouses';
import transactions from './transactions';

const customerReducer = combineReducers({
  list,
  warehouses,
  transactions,
});

export default customerReducer;
