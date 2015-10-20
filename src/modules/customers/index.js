import { combineReducers } from 'redux';
import customers from './customers';
import adding from './new';

const customerReducer = combineReducers({
  customers,
  adding
});

export default customerReducer;
