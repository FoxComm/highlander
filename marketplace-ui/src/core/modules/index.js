import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';
import { reducer as formReducer } from 'redux-form';

import { reducer as asyncReducer, getActionInProgress, getActionFailed } from './async-utils';
import { getApplyFormActionState } from './merchant-application';

const reducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
  form: formReducer,
});

export default reducer;

/** selectors */
export const getApplyFormActionInProgress = state => getActionInProgress(state.asyncActions)(getApplyFormActionState);
export const getApplyFormActionFailed = state => getActionFailed(state.asyncActions)(getApplyFormActionState);
