// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';


export const bulkRequest = createAction('BULK_REQUEST');
export const bulkDone = createAction('BULK_DONE', (successes, errors) => [successes, errors]);
export const bulkSetMessages = createAction('BULK_SET_MESSAGES');
export const bulkReset = createAction('BULK_RESET');
export const bulkClearSuccesses = createAction('BULK_CLEAR_SUCCESSES');
export const bulkClearErrors = createAction('BULK_CLEAR_ERRORS');

export function setMessages(messages) {
  return dispatch => {
    dispatch(bulkSetMessages(messages));
  };
}

export function reset() {
  return dispatch => {
    dispatch(bulkReset());
  };
}

export function clearSuccesses() {
  return dispatch => {
    dispatch(bulkClearSuccesses());
  };
}

export function clearErrors() {
  return dispatch => {
    dispatch(bulkClearErrors());
  };
}

const initialState = {
  isFetching: false,
  messages: {},
};

const reducer = createReducer({
  [bulkRequest]: (state) => {
    return {
      ...state,
      isFetching: true,
    };
  },
  [bulkDone]: (state, [successes, errors]) => {
    return {
      ...state,
      isFetching: false,
      successes: _.isEmpty(successes) ? null : successes,
      errors: _.isEmpty(errors) ? null : errors,
    };
  },
  [bulkSetMessages]: (state, messages) => {
    return {
      ...state,
      messages,
    };
  },
  [bulkReset]: () => {
    return {
      ...initialState,
    };
  },
  [bulkClearSuccesses]: (state) => {
    return _.omit(state, 'successes');
  },
  [bulkClearErrors]: (state) => {
    return _.omit(state, 'errors');
  },
}, initialState);

export default reducer;
