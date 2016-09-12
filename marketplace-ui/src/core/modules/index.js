import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';

import { reducer as asyncReducer } from './async-utils';

const reducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
});

export default reducer;
