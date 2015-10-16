
import { combineReducers } from 'redux';
import cards from './cards';
import adding from './new';
import transactions from './transactions';

const giftCardReducer = combineReducers({
  cards,
  adding,
  transactions
});

export default giftCardReducer;
