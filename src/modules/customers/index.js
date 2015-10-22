import { combineReducers } from 'redux';
import customers from './customers';
import adding from './new';
import details from './details';

const customerReducer = combineReducers({
  customers,
  adding,
  details
});

export default customerReducer;
