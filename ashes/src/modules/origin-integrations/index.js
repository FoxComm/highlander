import { combineReducers } from 'redux';
import details from './details';

const integrationReducer = combineReducers({
  details,
});

export default integrationReducer;
