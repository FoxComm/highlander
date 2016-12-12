import { combineReducers } from 'redux';
import details from './details';

const businessProfileReducer = combineReducers({
  details,
});

export default businessProfileReducer;