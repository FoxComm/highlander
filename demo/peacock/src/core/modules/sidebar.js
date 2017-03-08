/* @flow */

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

type FormData = {
  isVisible: boolean;
};

export const toggleSidebar = createAction('TOGGLE_SIDEBAR');

const initialState : FormData = {
  isVisible: false,
};

const reducer = createReducer({
  [toggleSidebar]: state => {
    const currentState = _.get(state, 'isVisible', false);
    return {
      ...state,
      isVisible: !currentState,
    };
  },
}, initialState);

export default reducer;
