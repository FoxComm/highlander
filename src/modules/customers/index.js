import { combineReducers } from 'redux';
import customers from './customers';
import adding from './new';
import details from './details';
import creditCards from './credit-cards';

const customerReducer = combineReducers({
  customers,
  adding,
  details,
  creditCards
});

export default customerReducer;
