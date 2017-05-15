/* eslint-disable import/no-named-as-default */

import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux';
import crossSell from './cross-sell';

import { reducer as asyncReducer } from '@foxcomm/wings/lib/redux/async-utils';

const reducer = combineReducers({
  routing: routeReducer,
  asyncActions: asyncReducer,
  crossSell,
});

export default reducer;
