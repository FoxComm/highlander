
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import _ from 'lodash';

import { updateUser } from './auth';

export const toggleNameModal = createAction('TOGGLE_NAME_MODAL');
export const toggleEmailModal = createAction('TOGGLE_EMAIL_MODAL');
export const togglePasswordModal = createAction('TOGGLE_PASSWORD_MODAL');
export const toggleAddressesModal = createAction('TOGGLE_ADDRESSES_MODAL');
export const toggleOrderDetails = createAction('TOGGLE_ORDER_DETAILS');

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
export const clearPasswordErrors = _changePassword.clearErrors;
export const clearAccountErrors = _updateAccount.clearErrors;

const initialState = {
  account: {},
  nameModalVisible: false,
  emailModalVisible: false,
  passwordModalVisible: false,
  addressesModalVisible: false,
  orderDetailsVisible: false,
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
  [toggleEmailModal]: (state) => {
    const current = _.get(state, 'emailModalVisible', false);
    return {
      ...state,
      emailModalVisible: !current,
    };
  },
  [togglePasswordModal]: (state) => {
    const current = _.get(state, 'passwordModalVisible', false);
    return {
      ...state,
      passwordModalVisible: !current,
    };
  },
  [toggleAddressesModal]: (state) => {
    const current = _.get(state, 'addressesModalVisible', false);
    return {
      ...state,
      addressesModalVisible: !current,
    };
  },
  [toggleOrderDetails]: (state) => {
    const current = _.get(state, 'orderDetailsVisible', false);
    return {
      ...state,
      orderDetailsVisible: !current,
    };
  },
}, initialState);

export default reducer;
