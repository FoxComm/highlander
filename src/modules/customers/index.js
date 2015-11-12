import { combineReducers } from 'redux';
import customers from './list';
import adding from './new';
import details from './details';

const customerReducer = combineReducers({
  customers,
  adding,
  details
});

export default customerReducer;
