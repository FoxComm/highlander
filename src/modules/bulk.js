// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';


export const bulkRequest = createAction('BULK_REQUEST');
export const bulkDone = createAction('BULK_DONE', (successes, errors) => [successes, errors]);
export const setMessages = createAction('BULK_SET_MESSAGES');
export const reset = createAction('BULK_RESET');
export const clearSuccesses = createAction('BULK_CLEAR_SUCCESSES');
export const clearErrors = createAction('BULK_CLEAR_ERRORS');


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
  [setMessages]: (state, messages) => {
    return {
      ...state,
      messages,
    };
  },
  [reset]: () => {
    return {
      ...initialState,
    };
  },
  [clearSuccesses]: (state) => {
    return _.omit(state, 'successes');
  },
  [clearErrors]: (state) => {
    return _.omit(state, 'errors');
  },
}, initialState);

export default reducer;
