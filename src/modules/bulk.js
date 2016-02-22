// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';


export const initialState = {
  isFetching: false,
  messages: {},
};

export const reducers = {
  bulkRequest: (state) => {
    return {
      ...state,
      isFetching: true,
    };
  },
  bulkDone: (state, [successes, errors]) => {
    return {
      ...state,
      isFetching: false,
      successes: _.isEmpty(successes) ? null : successes,
      errors: _.isEmpty(errors) ? null : errors,
    };
  },
  setMessages: (state, messages) => {
    return {
      ...state,
      messages,
    };
  },
  reset: () => {
    return {
      ...initialState,
    };
  },
  clearSuccesses: (state) => {
    return _.omit(state, 'successes');
  },
  clearErrors: (state) => {
    return _.omit(state, 'errors');
  },
};
