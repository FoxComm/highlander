
import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux';

import auth from './auth';

const reducer = combineReducers({
  routing: routeReducer,
  auth,
});

export default reducer;
