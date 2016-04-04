
import { combineReducers } from 'redux';
import list from './list';

const promotionsReducer = combineReducers({
  list,
});

export default promotionsReducer;
