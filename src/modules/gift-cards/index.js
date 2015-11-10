import { combineReducers } from 'redux';
import cards from './cards';
import adding from './new';
import details from './details';
import transactions from './transactions';

const giftCardReducer = combineReducers({
  cards,
  adding,
  details,
  transactions
});

export default giftCardReducer;
