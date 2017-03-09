import { combineReducers } from 'redux';
import list from '../skus/list';
import warehouses from '../skus/warehouses';
import transactions from '../skus/transactions';

const customerReducer = combineReducers({
  list,
  warehouses,
  transactions,
});

export default customerReducer;
