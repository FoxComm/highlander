import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';
import { reducer as formReducer } from 'redux-form';

import { reducer as asyncReducer, getActionInProgress, getActionFailed } from './async-utils';
import applicationReducer, { getApplicationActionNamespace } from './merchant-application';
import accountReducer, { getAccountActionNamespace } from './merchant-account';

const reducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
  form: formReducer,
  application: applicationReducer,
  account: accountReducer,
});

export default reducer;

/** selectors */
export const getApplicationInProgress = state => getActionInProgress(state.asyncActions, getApplicationActionNamespace());
export const getApplicationFailed = state => getActionFailed(state.asyncActions, getApplicationActionNamespace());

export const getAccountInProgress = state => getActionInProgress(state.asyncActions, getAccountActionNamespace());
export const getAccountFailed = state => getActionFailed(state.asyncActions, getAccountActionNamespace());
