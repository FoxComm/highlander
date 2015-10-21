'use strict';

import { createAction, createReducer } from 'redux-act';

export const setStart = createAction('SET_START');
export const setLimit = createAction('SET_LIMIT');

const initialState = {
  start: 0,
  limit: 25
};

const reducer = createReducer({
  [setStart]: (value) => {
    return {
      ...state,
      start: value
    };
  },
  [setLimit]: (state, value) => {
    return {
      ...state,
      limit: value
    };
  }
}, initialState);

export default reducer;
