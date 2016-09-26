import { combineReducers } from 'redux';
import details from './details';

const applicationReducer = combineReducers({
  details,
});

export default applicationReducer;
