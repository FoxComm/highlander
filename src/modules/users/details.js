/* @flow weak */

import Api from '../../lib/api';
import { createReducer } from 'redux-act';
import createAsyncActions from '../async-utils';

const getUser = createAsyncActions(
  'getUser',
  (id: string) => {
    return Api.get(`/store-admins/${id}`);
  }
);

const _updateUser = createAsyncActions(
  'updateUser',
  (user) => {
    const id = user.id;
    return Api.patch(`/store-admins/${id}`, user);
  }
);

export function fetchUser(id: string) {
  return dispatch => {
    dispatch(getUser.perform(id));
  };
}

export const updateUser = _updateUser.perform;

function updateUserInState(state, response) {
  return {
    ...state,
    ...response,
  };
}

const reducer = createReducer({
  [getUser.succeeded]: updateUserInState,
  [_updateUser.succeeded]: updateUserInState,
}, {});

export default reducer;
