import { combineReducers } from 'redux';
import details from './details';
import list from './list';

const skuReducer = combineReducers({
  details,
  list,
});

export default skuReducer;
