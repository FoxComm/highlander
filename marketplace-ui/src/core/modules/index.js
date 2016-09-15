import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';
import { reducer as formReducer } from 'redux-form';

import { reducer as asyncReducer, getActionInProgress, getActionFailed } from './async-utils';
import * as application from './merchant-application';
import accountReducer, { getAccountActionNamespace } from './merchant-account';
import infoReducer, { getInfoActionNamespace } from './merchant-info';

const reducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
  form: formReducer,
  application: application.default,
  account: accountReducer,
  info: infoReducer,
});

export default reducer;

/** selectors */
export const getMerchantId = state => application.getMerchantId(state.application);

export const getApplicationInProgress = state => getActionInProgress(state.asyncActions, application.getApplicationActionNamespace());
export const getApplicationFailed = state => getActionFailed(state.asyncActions, application.getApplicationActionNamespace());

export const getAccountInProgress = state => getActionInProgress(state.asyncActions, getAccountActionNamespace());
export const getAccountFailed = state => getActionFailed(state.asyncActions, getAccountActionNamespace());

export const getInfoInProgress = state => getActionInProgress(state.asyncActions, getInfoActionNamespace());
export const getInfoFailed = state => getActionFailed(state.asyncActions, getInfoActionNamespace());
