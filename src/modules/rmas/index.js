import { combineReducers } from 'redux';
import list from './list';
import details from './details';


const rmaReducer = combineReducers({
  list,
  details
});

export default rmaReducer;
