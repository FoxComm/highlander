import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux';

import { reducer as asyncReducer } from './async-utils';

const reducer = combineReducers({
  routing: routeReducer,
  asyncActions: asyncReducer,
});

export default reducer;
