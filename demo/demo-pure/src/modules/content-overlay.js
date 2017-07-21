/* @flow */

import { createAction, createReducer } from 'redux-act';

type FormData = {
  isVisible: boolean;
};

export const toggleContentOverlay = createAction('TOGGLE_CONTENT_OVERLAY');

const initialState : FormData = {
  isVisible: false,
};

const reducer = createReducer({
  [toggleContentOverlay]: (state, newValue) => {
    return {
      ...state,
      isVisible: newValue,
    };
  },
}, initialState);

export default reducer;
