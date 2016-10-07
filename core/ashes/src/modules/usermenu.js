/* @flow */

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

type FormData = {
  isVisible: boolean;
};

export const toggleUserMenu = createAction('TOGGLE_USER_MENU');

const initialState : FormData = {
  isVisible: false,
};

const reducer = createReducer({
  [toggleUserMenu]: state => {
    const currentState = _.get(state, 'isVisible', false);
    return {
      ...state,
      isVisible: !currentState,
    };
  },
}, initialState);

export default reducer;
