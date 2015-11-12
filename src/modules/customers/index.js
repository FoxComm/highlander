import { combineReducers } from 'redux';
import customers from './customers';
import adding from './new';
import details from './details';
import addresses from './addresses';

const customerReducer = combineReducers({
  customers,
  adding,
  details,
  addresses
});

export default customerReducer;
