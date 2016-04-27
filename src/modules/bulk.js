// libs
import _ from 'lodash';

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
  bulkError: (state, error) => {
    return {
      ...state,
      error: error
    };
  },
  clearError: state => {
    return {
      ...state,
      error: null
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
