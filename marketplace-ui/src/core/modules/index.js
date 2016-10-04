import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';
import { reducer as formReducer } from 'redux-form';

import { reducer as asyncReducer, getActionInProgress, getActionFailed, getActionSucceeded } from './async-utils';
import * as application from './merchant-application';
import * as account from './merchant-account';
import infoReducer, { getInfoActionNamespace } from './merchant-info';

const reducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
  form: formReducer,
  application: application.default,
  accounts: account.default,
  info: infoReducer,
});

export default reducer;

/** selectors */

export const getApplication = state => application.getApplication(state.application);
export const getAccounts = state => account.getAccounts(state.accounts);

export const getApplicationFetchFailed = state =>
  getActionFailed(state.asyncActions, application.getApplicationFetchActionNamespace());

export const getApplicationInProgress = state =>
  getActionInProgress(state.asyncActions, application.getApplicationActionNamespace());

export const getApplicationFailed = state =>
  getActionFailed(state.asyncActions, application.getApplicationActionNamespace());

export const getAccountInProgress = state =>
  getActionInProgress(state.asyncActions, account.getAccountActionNamespace());

export const getAccountFailed = state =>
  getActionFailed(state.asyncActions, account.getAccountActionNamespace());

export const getInfoInProgress = state =>
  getActionInProgress(state.asyncActions, getInfoActionNamespace());

export const getInfoFailed = state =>
  getActionFailed(state.asyncActions, getInfoActionNamespace());
export const getInfoDone = state =>
  getActionSucceeded(state.asyncActions, getInfoActionNamespace());
