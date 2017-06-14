
import { combineReducers } from 'redux';
import list from './list';
import bulk from './bulk';
import details from './details';

const contentTypesReducer = combineReducers({
  list,
  bulk,
  details,
});

export default contentTypesReducer;
