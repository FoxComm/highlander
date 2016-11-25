/* @flow */

import { createAction, createReducer } from 'redux-act';

type FormData = {
  isVisible: boolean;
};

export const closeBanner = createAction('CLOSE_BANNER');

const initialState : FormData = {
  isVisible: true,
};

const reducer = createReducer({
  [closeBanner]: state => {
    return {
      ...state,
      isVisible: false,
    };
  },
}, initialState);

export default reducer;
