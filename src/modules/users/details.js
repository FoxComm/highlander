/* @flow weak */

import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import createAsyncActions from '../async-utils';

import { configureUserState, configureUserData, createEmptyUser } from '../../paragons/user';

const _getUser = createAsyncActions(
  'getUser',
  (id: string) => {
    return Api.get(`/store-admins/${id}`);
  }
);

const _updateUser = createAsyncActions(
  'updateUser',
  (user) => {
    const id = user.id;
    const data = configureUserData(user);
    return Api.patch(`/store-admins/${id}`, data);
  }
);

export function fetchUser(id: string) {
  return dispatch => {
    if (id.toLowerCase() == 'new') {
      dispatch(userNew());
    } else {
      dispatch(_getUser.perform(id));
    }
  };
}

export const updateUser = _updateUser.perform;
export const userNew = createAction('USER_NEW');

function updateUserInState(state, response) {
  return {
    ...state,
    user: configureUserState(response)
  };
}

const reducer = createReducer({
  [userNew]: state => {
    return {
      ...state,
      user: createEmptyUser(),
    };
  },
  [_getUser.succeeded]: updateUserInState,
  [_updateUser.succeeded]: updateUserInState,
}, {});

export default reducer;
