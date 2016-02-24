import { combineReducers } from 'redux';
import details from './details';

const productReducer = combineReducers({
  details,
});

export default productReducer;
