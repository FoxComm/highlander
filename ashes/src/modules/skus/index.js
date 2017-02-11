import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import transactions from './transactions';
import warehouses from './warehouses';

const skuReducer = combineReducers({
  list,
  details,
  transactions,
  warehouses,
});

export default skuReducer;
