import { combineReducers } from 'redux';
import list from './list';
import warehouses from './warehouses';

const customerReducer = combineReducers({
  list,
  warehouses,
});

export default customerReducer;
