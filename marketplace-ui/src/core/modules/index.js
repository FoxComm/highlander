import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';
import { reducer as formReducer } from 'redux-form';

import { reducer as asyncReducer, getActionState, getActionInProgress, getActionFailed } from './async-utils';
import { getApplyFormActionNamespace } from './merchant-application';

const reducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
  form: formReducer,
});

export default reducer;



/** selectors */
export const getApplyFormActionInProgress = state => getActionInProgress(state.asyncActions, getApplyFormActionNamespace());
export const getApplyFormActionFailed = state => getActionFailed(state.asyncActions, getApplyFormActionNamespace());
