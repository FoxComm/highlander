
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import _ from 'lodash';

import { updateUser } from './auth';

export const toggleNameModal = createAction('TOGGLE_NAME_MODAL');

const _fetchAccount = createAsyncActions(
  'fetchAccount',
  function() {
    return this.api.account.get();
  }
);

const _updateAccount = createAsyncActions(
  'updateAccount',
  function(payload) {
    return this.api.account.update(payload).then((account) => {
      this.dispatch(updateUser(account));
      return account;
    });
  }
);

const _changePassword = createAsyncActions(
  'changePassword',
  function(oldPassword: string, newPassword: string) {
    return this.api.account.changePassword(oldPassword, newPassword);
  }
);

export const fetchAccount = _fetchAccount.perform;
export const updateAccount = _updateAccount.perform;
export const changePassword = _changePassword.perform;

const initialState = {
  account: {},
  nameModalVisible: false,
};

function updateAccountInState(state, account) {
  return {
    ...state,
    account,
  };
}

const reducer = createReducer({
  [_fetchAccount.succeeded]: updateAccountInState,
  [_updateAccount.succeeded]: updateAccountInState,
  [toggleNameModal]: (state) => {
    const current = _.get(state, 'nameModalVisible', false);
    return {
      ...state,
      nameModalVisible: !current,
    };
  },
}, initialState);

export default reducer;
