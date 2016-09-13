import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';
import { reducer as formReducer } from 'redux-form';

import { reducer as asyncReducer } from './async-utils';

const reducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
  form: formReducer,
});

export default reducer;
