import { combineReducers } from 'redux';
import list from './list';

const customerReducer = combineReducers({
  list,
});

export default customerReducer;
