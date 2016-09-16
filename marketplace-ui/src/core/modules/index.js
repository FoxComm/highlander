import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';
import { reducer as formReducer } from 'redux-form';

import { reducer as asyncReducer, getActionInProgress, getActionFailed } from './async-utils';
import * as application from './merchant-application';
import * as account from './merchant-account';
import infoReducer, { getInfoActionNamespace } from './merchant-info';

const reducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
  form: formReducer,
  application: application.default,
  account: account.default,
  info: infoReducer,
});

export default reducer;

/** selectors */

export const getApplicationId = state => application.getApplicationId(state.application);
export const getAccountId = state => account.getAccountId(state.account);

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

export const getActiveStep = state => {

}
