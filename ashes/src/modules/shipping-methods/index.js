import { combineReducers } from 'redux';
import details from './details';
import list from './list';

const shippingMethodReducer = combineReducers({
  details,
  list,
});

export default shippingMethodReducer;
