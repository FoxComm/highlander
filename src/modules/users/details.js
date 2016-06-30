/* @flow weak */

import Api from '../../lib/api';
import { createReducer } from 'redux-act';
import createAsyncActions from '../async-utils';

import { configureUserState, configureUserData} from '../../paragons/user';

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
    const data = configureUserData(user);
    return Api.patch(`/store-admins/${id}`, data);
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
    user: configureUserState(response)
  };
}

const reducer = createReducer({
  [getUser.succeeded]: updateUserInState,
  [_updateUser.succeeded]: updateUserInState,
}, {});

export default reducer;
