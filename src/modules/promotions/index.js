
import { combineReducers } from 'redux';
import list from './list';
import bulk from './bulk';

const promotionsReducer = combineReducers({
  list,
  bulk,
});

export default promotionsReducer;
