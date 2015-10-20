import { combineReducers } from 'redux';
import customers from './customers';

const customerReducer = combineReducers({
  customers
});

export default customerReducer;
