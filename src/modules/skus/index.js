import { combineReducers } from 'redux';
import list from './list';

const skuReducer = combineReducers({
  list,
});

export default skuReducer;
