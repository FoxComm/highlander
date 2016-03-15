import { combineReducers } from 'redux';
import details from './details';
import list from './list';

const productReducer = combineReducers({
  details,
  list,
});

export default productReducer;
